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

  val LinesPerHalfHour: Int = LinesPerHour / 2
  val HalfHoursPerDay: Int = HoursPerDay * 2
  val StartDate: LocalDate = LocalDate.of(2021, 1, 1)
  val EndDate: LocalDate = LocalDate.now()

  case class Author(name: String)
  case class GitCommit(oid: String, author: Author, message: String, additions: Int, deletions: Int, authoredDate: LocalDateTime)

  case class GitData(projectName: String, oid: String, author: String, message: String, additions: Int, deletions: Int, authoredDate: LocalDateTime) {
    def show = s"[${message.take(10)}(+${additions},-${deletions})]"
  }
  object GitData {
    def fromCommit(projectName: String, c: GitCommit) =
      GitData(projectName, c.oid, c.author.name, c.message, c.additions, c.deletions, c.authoredDate)
  }

  case class WorkToDo(onDate: LocalDate, halfHours: Int, commit: GitData) {
    def show = s"[${commit.message.take(10)}(wk${halfHours})]"
  }
  object WorkToDo {
    def apply(commit: GitData) = {
      val linesPerHalfHour: Double = LinesPerHour.toDouble / 2
      val taskHalfHours = Math.ceil(commit.additions.toDouble / linesPerHalfHour).toInt
      new WorkToDo(commit.authoredDate.toLocalDate, taskHalfHours, commit)
    }
  }

  case class WorkDone(halfHoursWorked: Int, workToDo: WorkToDo) {
    def show = s"[(wk${workToDo.halfHours}-done${halfHoursWorked})${workToDo.commit.message.take(10)}]"
    def remaining = workToDo.halfHours - halfHoursWorked
    def complete = {
      val r = remaining
      if (r < 0) throw new IllegalArgumentException(s"Less than 0 work remaining ${remaining} on ${this.workToDo.commit.message.take(5)}")
      r == 0
    }
    def doWork(slots: Int) =
      this.copy(this.halfHoursWorked + 1, this.workToDo)
    def remainingWork = workToDo.copy(halfHours = workToDo.halfHours - halfHoursWorked)
  }

  case class Day(date: LocalDate, work: List[WorkToDo], halfHoursLeft: Int) {
    def show = {
      s"== Day: ${date} == ${work.map(w => s"${w.halfHours}-[${w.commit.message.take(20)}:${w.commit.additions}]")}"
    }
  }
  case class Calendar(start: LocalDate, end: LocalDate, days: mutable.LinkedHashMap[LocalDate, Day], allWorkDone: Map[LocalDate, List[WorkDone]], daysOriginal: mutable.LinkedHashMap[LocalDate, Day]) {
    def show = {
      days.map {
        case (date, day) =>
          val dayOriginal = daysOriginal(date)
          val workDone = allWorkDone.get(date).getOrElse(List())
          val workAndDone = dayOriginal.work.map(wk => (wk, workDone.find(_.workToDo.commit == wk.commit)))
          val workAndDoneStr =
            workAndDone.map { case (origWork, workDoneOpt) =>
              if (workDoneOpt.isDefined) {
                val workDone = workDoneOpt.get
                s"Wk${origWork.halfHours}<-${workDone.halfHoursWorked}r${workDone.remaining}-[${origWork.commit.message.take(10)}]"
              } else {
                s"Wk${origWork.halfHours}-[${origWork.commit.message.take(10)}]"
              }
            }
          s"== ${date} == HH:${workDone.map(_.halfHoursWorked).sum} == ${workAndDoneStr}"
      }.mkString("\n")
    }

    /**
     * TODO check if its before start and/or after end and throw an error also maybe check if
     *      work is being moved back before the start date
     */
    def doWorkOn(date: LocalDate): Calendar = {
      // get all the work that needs to be done on that date
      val currDay = days(date)
      val workForDay = currDay.work

      // round robbin the half hour slots assigning them to each work item until no slots are left for the day or all the work is done
      // - for the work done, return a list of WorkDone objects (return WorkDone of 0 if no work could be done on that date)
      // - compute a List[WorkToDo] of remaining half-hour slots for each task
      var worksDone = workForDay.map(WorkDone(0, _))
      var slotsRemaining = currDay.halfHoursLeft

      println(s"====== Begin ${workForDay.length} works on ${currDay.date} with ${slotsRemaining} slots remaining: ${workForDay.map(_.show)}")

      while (!worksDone.forall(_.complete) && slotsRemaining > 0) {
        worksDone =
          worksDone.map { workDone =>
            if (slotsRemaining > 0 && !workDone.complete) {
              val newWorkDone = workDone.doWork(1)
              slotsRemaining = slotsRemaining - 1
              println(s"== ${date} == Doing 1 Work for: done${workDone.halfHoursWorked}=>${newWorkDone.show} ======= Remaining: ${slotsRemaining} ")
              newWorkDone
            } else {
              workDone
            }
          }
      }

      // TODO Renamed Done to Performed (Done here is as-in something was done as opposed to completed)
      val worksFinished = worksDone.filter(_.complete)
      // all the work items with remaining work (i.e. whose remaining slots are greater than zero)
      val incompleteWorksPerformed = worksDone.filterNot(_.complete)
      val remainingWork = incompleteWorksPerformed.map(_.remainingWork)

      if (worksDone.forall(_.complete) && slotsRemaining > 0) {
        println(s"!!!! All ${worksDone.length} works done for ${currDay.date} and ${slotsRemaining} slots remaining: ${worksDone.map(_.show)}")
      } else if (!worksDone.forall(_.complete) && slotsRemaining == 0) {
        println(s"----- ${worksFinished.length}:${worksFinished.map(_.show)} are done and 0 slots remaining ${incompleteWorksPerformed.map(_.show)} additional work: ${remainingWork.map(_.show)}")
      } else {
        println(s"????? Odd case. Slots remaining ${slotsRemaining} ===== and Works done are: ${worksDone.map(w => (w.complete, w.workToDo.commit.show))}")
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

      val newWorksDone: Map[LocalDate, List[WorkDone]] = allWorkDone ++ Map(currDay.date -> worksDone)


      // get the previous day
      // TODO skip a day of it is a friday (since not working friday nights) or holiday (need to get those dates) then move to the day before
      // add remaining work items List[WorkToDo] to that day and return a new calendar representing that info

      Calendar(start, end, newDays, newWorksDone, days)
    }
  }

  def makeDateRange(start: LocalDate, end: LocalDate) =
    Iterator.iterate(StartDate)(date => date.plusDays(1)).takeWhile(_.isBefore(EndDate)).toList

  object Calendar {
    def build(allCommits: List[GitData]) = {
      val workByDay = allCommits.map(c => WorkToDo(c)).groupBy(_.onDate)
      val dateRange = makeDateRange(StartDate, EndDate)

      val daysList =
        dateRange.map { date =>
          val work = workByDay.get(date).getOrElse(List())
          (date, Day(date, work, HalfHoursPerDay))
        }

      val daysMap = mutable.LinkedHashMap(daysList: _*)
      Calendar(StartDate, EndDate, daysMap, Map(), daysMap.clone())
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
    val commitsRaw = mapper.readValue[List[GitCommit]](new File(path))
    val commits = commitsRaw.map(r => GitData.fromCommit("zio-protoquill", r))

    val filtered =
      commits
        .sortBy(_.authoredDate.toInstant(ZoneOffset.UTC))
        .filter(_.author == "Alexander Ioffe")
        .filter(_.authoredDate.isAfter(LocalDate.of(2021,1,1).atStartOfDay()))
        .filter(_.additions > LinesPerHalfHour /*Filter by commits with at least half an hour of work*/)
        //.map(_.additions)


    val totalAdditions = filtered.map(_.additions).sum.toDouble
    val totalHoursWorked = totalAdditions/LinesPerHour
    println(s"Total Since 2021: ${totalHoursWorked}")

//    def toHours(additions: Int) = additions.toDouble/(LinesPerHour.toDouble)
//    filtered.map(f => s"====== Add: ${f.additions} Work: ${toHours(f.additions)} ======= ${f.authoredDate} ==== ${f.message.take(10)}").foreach(println(_))

    val cal = Calendar.build(filtered)
    println(cal.show)

    //val dates = makeDateRange(LocalDate.of(2022,6,8), LocalDate.of(2022,5,23))
    val dates = makeDateRange(LocalDate.of(2022,6,7), LocalDate.of(2022,6,1))
    val newCal = dates.foldLeft(cal)((cal, date) => cal.doWorkOn(date))
    println(cal.show)

    //val newCal = cal.doWorkOn(LocalDate.of(2022,6,8))
    //val newCal = cal.doWorkOn(LocalDate.of(2022,6,7))
  }
}
