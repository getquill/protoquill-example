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

  val LinesPerHour: Int = 50
  val HoursPerDay = 5
  val SlotsPerHour = 2

  val LinesPerSlot: Int = LinesPerHour / 2
  val SlotsPerDay: Int = HoursPerDay * 2
  val StartDate: LocalDate = LocalDate.of(2021, 1, 1)
  val EndDate: LocalDate = LocalDate.now()

  case class Author(name: String)
  case class GitCommit(oid: String, author: Author, message: String, additions: Int, deletions: Int, authoredDate: LocalDateTime)

  case class GitData(projectName: String, oid: String, author: String, message: String, additions: Int, deletions: Int, authoredDate: LocalDateTime) {
    def showAddsDeletes = s"[${message.take(10)}(+${additions},-${deletions})]"
    val projectNameLetter = if (projectName == "zio-quill") "Q" else if (projectName == "zio-protoquill") "P" else throw new IllegalArgumentException("")
    def show = s"${projectNameLetter}:${message.take(10)}"
  }
  object GitData {
    def fromCommit(projectName: String, c: GitCommit) =
      GitData(projectName, c.oid, c.author.name, c.message, c.additions, c.deletions, c.authoredDate)
  }

  case class WorkToDo(onDate: LocalDate, workSlots: Int, commit: GitData) {
    def show = s"${commit.show}(wk${workSlots})"
  }
  object WorkToDo {
    def apply(commit: GitData) = {
      val linesPerSlot: Double = LinesPerHour.toDouble / SlotsPerHour
      val slotsNeededByTask = Math.ceil(commit.additions.toDouble / linesPerSlot).toInt
      new WorkToDo(commit.authoredDate.toLocalDate, slotsNeededByTask, commit)
    }
  }

  case class WorkDone(slotsWorked: Int, workToDo: WorkToDo) {
    def show = s"[(wk${workToDo.workSlots}-done${slotsWorked})${workToDo.commit.show}]"
    def remaining = workToDo.workSlots - slotsWorked
    def complete = {
      val r = remaining
      if (r < 0) throw new IllegalArgumentException(s"Less than 0 work remaining ${remaining} on ${this.workToDo.commit.message.take(5)}")
      r == 0
    }
    def doWork(slots: Int) =
      this.copy(this.slotsWorked + 1, this.workToDo)
    def remainingWork = workToDo.copy(workSlots = workToDo.workSlots - slotsWorked)
  }

  case class Day(date: LocalDate, work: List[WorkToDo], workSlotsLeft: Int) {
    def show = {
      s"== Day: ${date} == ${work.map(w => s"${w.workSlots}-[${w.commit.message.take(20)}:${w.commit.additions}]")}"
    }
  }
  case class Calendar(start: LocalDate, end: LocalDate, days: mutable.LinkedHashMap[LocalDate, Day], allWorkDone: Map[LocalDate, List[WorkDone]], daysOriginal: mutable.LinkedHashMap[LocalDate, Day]) {
    def show = {
      days.toList.sortBy(_._1.atStartOfDay().toInstant(ZoneOffset.UTC)).map {
        case (date, day) =>
          val dayOriginal = daysOriginal.get(date).getOrElse(Day(date, List(), SlotsPerDay))
          val workDone = allWorkDone.get(date).getOrElse(List())
          val workAndDone = dayOriginal.work.map(wk => (wk, workDone.find(_.workToDo.commit == wk.commit)))
          val workAndDoneStr =
            workAndDone.map { case (origWork, workDoneOpt) =>
              if (workDoneOpt.isDefined) {
                val workDone = workDoneOpt.get
                s"Wk${origWork.workSlots}<-${workDone.slotsWorked}r${workDone.remaining}-[${origWork.show}]"
              } else {
                s"Wk${origWork.workSlots}-[${origWork.commit.show}]"
              }
            }
          s"== ${date} == HHWorked:${workDone.map(_.slotsWorked).sum} == ${workAndDoneStr}"
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
      var slotsRemaining = currDay.workSlotsLeft

      println(s"====== Begin ${workForDay.length} works on ${currDay.date} with ${slotsRemaining} slots remaining: ${workForDay.map(_.show)}")

      while (!worksDone.forall(_.complete) && slotsRemaining > 0) {
        worksDone =
          worksDone.map { workDone =>
            if (slotsRemaining > 0 && !workDone.complete) {
              val newWorkDone = workDone.doWork(1)
              slotsRemaining = slotsRemaining - 1
              println(s"== ${date} == Doing 1 Work for: done${workDone.slotsWorked}=>${newWorkDone.show} ======= Remaining: ${slotsRemaining} ")
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
      val prevDayRaw = days.get(prevDate).getOrElse(Day(prevDate, List(), SlotsPerDay))
      val prevDay = prevDayRaw.copy(work = remainingWork ++ prevDayRaw.work)

      newDays.put(currDay.date, currDay.copy(workSlotsLeft = slotsRemaining))
      newDays.put(prevDate, prevDay)

      val newWorksDone: Map[LocalDate, List[WorkDone]] = allWorkDone ++ Map(currDay.date -> worksDone)


      // get the previous day
      // TODO skip a day of it is a friday (since not working friday nights) or holiday (need to get those dates) then move to the day before
      // add remaining work items List[WorkToDo] to that day and return a new calendar representing that info

      Calendar(start, end, newDays, newWorksDone, days)
    }
  }

  def makeDateRange(start: LocalDate, end: LocalDate) =
    Iterator.iterate(start)(date => date.plusDays(1)).takeWhile(r => r.isBefore(end) || r == end).toList

  object Calendar {
    def build(allCommits: List[GitData]) = {
      val workByDay = allCommits.map(c => WorkToDo(c)).groupBy(_.onDate)
      val dateRange = makeDateRange(StartDate, EndDate)

      val daysList =
        dateRange.map { date =>
          val work = workByDay.get(date).getOrElse(List())
          (date, Day(date, work, SlotsPerDay))
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


  def loadFromPath(path: String, projectName: String): List[GitData] = {
    val commitsRaw = mapper.readValue[List[GitCommit]](new File(path))
    val commits = commitsRaw.map(r => GitData.fromCommit(projectName, r))
    commits
  }

  def main(args: Array[String]): Unit = {
//    val path = "/home/alexi/github_scripts/complete_commitdata_protoquill.json"
//    val commitsRaw = mapper.readValue[List[GitCommit]](new File(path))
//    val commits = commitsRaw.map(r => GitData.fromCommit("zio-protoquill", r))

    val commits =
      loadFromPath("/home/alexi/github_scripts/complete_commitdata_protoquill.json", "zio-protoquill") ++
        loadFromPath("/home/alexi/github_scripts/complete_commitdata_quill.json", "zio-quill")

    val filtered =
      commits
        .sortBy(_.authoredDate.toInstant(ZoneOffset.UTC))
        .filter(_.author == "Alexander Ioffe")
        .filter(_.authoredDate.isAfter(LocalDate.of(2021,1,1).atStartOfDay()))
        .filter(_.additions > LinesPerSlot /*Filter by commits with at least half an hour of work*/)
        //.map(_.additions)

    def totalHours(data: List[GitData]) = {
      val totalAdditions = data.map(_.additions).sum.toDouble
      val totalHoursWorked = totalAdditions/LinesPerHour
      totalHoursWorked
    }


    println(s"Total Hours Quill: ${totalHours(filtered.filter(_.projectName == "zio-quill"))}")
    println(s"Total Hours ProtoQuill: ${totalHours(filtered.filter(_.projectName == "zio-protoquill"))}")


    val cal = Calendar.build(filtered)
    val dates = makeDateRange(LocalDate.of(2021,1,1), LocalDate.of(2022,6,24)).reverse
    println(dates)
    val newCal = dates.foldLeft(cal)((cal, date) => cal.doWorkOn(date))
    println(newCal.show)

    val totalSlotsWorked = newCal.allWorkDone.map { case (date, workDone) => workDone.map(_.slotsWorked).sum }.sum
    val totalHoursWorked = totalSlotsWorked/2
    println(s"============= Worked: ${totalHoursWorked}, slots: ${totalSlotsWorked} =============")

    // TODO Go through the date range. Combine all entries from a single day.
    // create the CSV used for toggl
  }

  //    def toHours(additions: Int) = additions.toDouble/(LinesPerHour.toDouble)
  //    filtered.map(f => s"====== Add: ${f.additions} Work: ${toHours(f.additions)} ======= ${f.authoredDate} ==== ${f.message.take(10)}").foreach(println(_))
}
