object ScalaExample {
  // Case class for student records - compact, immutable data structure
  case class Student(
    id: String, 
    name: String, 
    age: Int, 
    subjects: List[Subject]
  ) {
    // Method to calculate overall average grade
    def calculateAverage(): Double = {
      if (subjects.isEmpty) 0.0
      else subjects.map(_.marks).sum / subjects.size
    }
    
    // Method to determine pass/fail status
    def isPassed(): Boolean = calculateAverage() >= 40.0
    
    // Get grade letter based on average
    def getGradeLetter(): String = {
      val avg = calculateAverage()
      avg match {
        case a if a >= 90 => "A+"
        case a if a >= 80 => "A"
        case a if a >= 70 => "B"
        case a if a >= 60 => "C"
        case a if a >= 50 => "D"
        case a if a >= 40 => "E"
        case _ => "F"
      }
    }
  }
  
  // Case class for subject information
  case class Subject(name: String, marks: Double)
  
  def main(args: Array[String]): Unit = {
    // Create a list of students
    val students = List(
      Student("S001", "John Doe", 20, 
        List(
          Subject("Mathematics", 85.5),
          Subject("Computer Science", 92.0),
          Subject("Physics", 78.5)
        )
      ),
      Student("S002", "Jane Smith", 19, 
        List(
          Subject("Mathematics", 90.0),
          Subject("Computer Science", 95.5),
          Subject("Physics", 88.0)
        )
      ),
      Student("S003", "Alex Brown", 21, 
        List(
          Subject("Mathematics", 62.5),
          Subject("Computer Science", 58.0),
          Subject("Physics", 55.0)
        )
      )
    )
    
    // Print student information
    println("Student Information:")
    students.foreach { student =>
      println(s"ID: ${student.id}, Name: ${student.name}, Age: ${student.age}")
      println(s"Subjects:")
      student.subjects.foreach { subject =>
        println(f"  ${subject.name}%-20s ${subject.marks}%.1f")
      }
      println(f"Average: ${student.calculateAverage()}%.2f")
      println(s"Grade: ${student.getGradeLetter()}")
      println(s"Status: ${if (student.isPassed()) "Passed" else "Failed"}")
      println()
    }
    
    // Demonstrating Scala's functional capabilities
    
    // 1. Find top performing student
    val topStudent = students.maxBy(_.calculateAverage())
    println(s"Top performing student: ${topStudent.name} with average ${topStudent.calculateAverage()}")
    
    // 2. Group students by grade letter
    val studentsByGrade = students.groupBy(_.getGradeLetter())
    println("\nStudents by grade:")
    studentsByGrade.foreach { case (grade, studentList) =>
      println(s"Grade $grade: ${studentList.map(_.name).mkString(", ")}")
    }
    
    // 3. Calculate average marks in each subject across all students
    val allSubjects = students.flatMap(_.subjects)
    val subjectAverages = allSubjects
      .groupBy(_.name)
      .map { case (name, subjects) => 
        (name, subjects.map(_.marks).sum / subjects.size) 
      }
    
    println("\nSubject averages:")
    subjectAverages.foreach { case (subject, avg) =>
      println(f"$subject%-20s $avg%.2f")
    }
  }
}



