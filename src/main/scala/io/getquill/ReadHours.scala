package io.getquill

import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.datatype.jsr310.{JSR310Module, JavaTimeModule}
import com.fasterxml.jackson.module.scala.{ClassTagExtensions, DefaultScalaModule}

import java.io.File
import java.time._
import scala.collection.mutable


object ReadHours {

  /*
    {
    "oid": "c1fa5de6d1abe9f377195840508069cb978f8763",
    "author": {
      "name": "Alexander Ioffe"
    },
    "message": "Try another fix",
    "additions": 4,
    "deletions": 3,
    "authoredDate": "2021-06-02T16:58:26Z"
  }
   */

  val LinesPerHour: Int = 40
  val HoursPerDay = 5
  val HalfHoursPerDay = HoursPerDay * 2
  val StartDate: LocalDate = LocalDate.of(2021, 1, 1)
  val EndDate: LocalDate = LocalDate.now()

  case class Author(name: String)
  case class GitCommit(oid: String, author: Author, message: String, additions: Int, deletions: Int, authoredDate: LocalDateTime)

  case class GitData(projectName: String, oid: String, author: String, message: String, additions: Int, deletions: Int, authoredDate: LocalDateTime)
  object GitData {
    def fromCommit(projectName: String, c: GitCommit) =
      GitData(projectName, c.oid, c.author.name, c.message, c.additions, c.deletions, c.authoredDate)
  }

  case class WorkToDo(onDate: LocalDate, halfHours: Int, commit: GitData)
  object WorkToDo {
    def apply(commit: GitData) = {
      val linesPerHalfHour: Double = LinesPerHour.toDouble / 2
      val taskHalfHours = Math.ceil(commit.additions.toDouble / linesPerHalfHour).toInt
      WorkToDo(commit.authoredDate.toLocalDate, taskHalfHours, commit)
    }
  }

  case class WorkDone(halfHoursWorked: Int, workToDo: WorkToDo) {
    def remaining = workToDo.halfHours - halfHoursWorked
    def complete = {
      val r = remaining
      if (r > 0) throw new IllegalArgumentException(s"Less than 0 work remaining ${remaining} on ${this.workToDo.commit.message.take(5)}")
      r == 0
    }
    def doWork(slots: Int) =
      this.copy(this.halfHoursWorked + 1, this.workToDo)
    def remainingWork = workToDo.copy(halfHours = workToDo.halfHours - halfHoursWorked)
  }

  case class Day(date: LocalDate, work: List[WorkToDo], halfHoursLeft: Int)
  case class Calendar(start: LocalDate, end: LocalDate, days: mutable.LinkedHashMap[LocalDate, Day]) {
    /**
     * TODO check if its before start and/or after end and throw an error also maybe check if
     *      work is being moved back before the start date
     */
    def doWorkOn(date: LocalDate): (Calendar, List[WorkDone]) = {
      // get all the work that needs to be done on that date
      val currDay = days(date)
      val workForDay = currDay.work

      // round robbin the half hour slots assigning them to each work item until no slots are left for the day or all the work is done
      // - for the work done, return a list of WorkDone objects (return WorkDone of 0 if no work could be done on that date)
      // - compute a List[WorkToDo] of remaining half-hour slots for each task
      var worksDone = workForDay.map(WorkDone(0, _))
      var slotsRemaining = currDay.halfHoursLeft


      while (!worksDone.forall(_.complete) && slotsRemaining > 0) {
        worksDone =
          worksDone.map { workDone =>
            if (slotsRemaining > 0 && !workDone.complete) {
              val newWorkDone = workDone.doWork(1)
              slotsRemaining = slotsRemaining - 1
              println(s"== ${currDay} == Doing 1 Work for: ${workDone.workToDo.commit.message.take(10)} ======= Remaining: ${slotsRemaining} ")
              newWorkDone
            } else {
              workDone
            }
          }
      }

      val worksFinished = worksDone.filter(_.complete)
      // all the work items with remaining work (i.e. whose remaining slots are greater than zero)
      val remainingWork = worksDone.filterNot(_.complete).map(_.remainingWork)

      if (worksDone.forall(_.complete) && slotsRemaining > 0) {
        println(s"!!!! All ${worksDone.length} works done for ${currDay} and ${slotsRemaining} slots remaining: ${worksDone.map(_.workToDo.commit.message.take(5))}")
      } else if (!worksDone.forall(_.complete) && slotsRemaining == 0) {
        println(s"----- ${worksFinished.length} are done and 0 slots remaining ${remainingWork.map(_.commit.message.take(10))}")
      } else {
        println(s"????? Odd case. Slots remaining ${slotsRemaining} ===== and Works done are: ${worksDone.map(w => (w.complete, w.workToDo.commit.message.take(5)))}")
      }

      val newDays = days.clone()
      // TODO Need to compute by skipping fridays holidays etc...
      //      for now maybe change the friday worked hours to be during the day & just filter out local holidays
      //      on the final output report?
      val prevDate = currDay.date.minusDays(1)
      val prevDayRaw = days.get(prevDate).getOrElse(Day(prevDate, List(), HalfHoursPerDay))
      val prevDay = prevDayRaw.copy(work = remainingWork ++ prevDayRaw.work)

      newDays.put(currDay.date, currDay.copy(halfHoursLeft = slotsRemaining))
      newDays.put(prevDate, prevDay)


      // get the previous day
      // TODO skip a day of it is a friday (since not working friday nights) or holiday (need to get those dates) then move to the day before
      // add remaining work items List[WorkToDo] to that day and return a new calendar representing that info

      (Calendar(start, end, days), worksDone)
    }
  }

  object Calendar {
    def build(allCommits: List[GitData]) = {
      val workByDay = allCommits.map(c => WorkToDo(c)).groupBy(_.onDate)
      val dateRange = Iterator.iterate(StartDate)(date => date.plusDays(1)).takeWhile(_.isBefore(EndDate)).toList

      val daysList =
        dateRange.map { date =>
          val work = workByDay.get(date).getOrElse(List())
          (date, Day(date, work))
        }
      Calendar(StartDate, EndDate, mutable.LinkedHashMap(daysList: _*))
    }
  }

  //case class CursorGroup(cursors: List[Cursor])

  val mapper =
    JsonMapper.builder()
      .addModule(DefaultScalaModule)
      .addModule(new JavaTimeModule())
      .build() :: ClassTagExtensions

  def main(args: Array[String]): Unit = {
    val path = "/home/alexi/github_scripts/complete_commitdata_protoquill.json"
    val commits = mapper.readValue[List[GitCommit]](new File(path))
    val filtered =
      commits
        .filter(_.authoredDate.isAfter(LocalDate.of(2021,1,1).atStartOfDay()))
        .filter(_.additions > 20)
        //.map(_.additions)


    val totalAdditions = filtered.map(_.additions).sum.toDouble
    val linesPerHour = 40.toDouble
    val totalHoursWorked = totalAdditions/linesPerHour
    println(s"Total Since 2021: ${totalHoursWorked}")

    filtered.map(f => s"====== ${f.additions} ======= ${f.authoredDate}").foreach(println(_))

    // combine entries on per day basis into super-commits
    // spread them over the previous N days at rate of lines-per-hour
    // - each of this will be a work-chains spread it out of last N days
    // examine all work chains, are there collisions?
    // - if so then I'm working 'simultaneously' and spread it out further back
  }
}
