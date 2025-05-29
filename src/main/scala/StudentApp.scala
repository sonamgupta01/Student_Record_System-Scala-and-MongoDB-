import org.mongodb.scala._
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.model.Updates._
import org.mongodb.scala.model.Sorts._
import org.mongodb.scala.result.{DeleteResult, InsertOneResult, UpdateResult}
import org.mongodb.scala.bson.{BsonDocument, Document}
import java.util.logging.{Level, Logger}
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import scala.io.StdIn
import scala.collection.JavaConverters._

// Student case class - simplified for easier implementation
case class Student(
  id: String,
  name: String,
  age: Int,
  courses: List[Course]
) {
  // Calculate overall average
  def calculateOverallAverage(): Double = {
    if (courses.isEmpty) 0.0
    else courses.map(_.marks).sum / courses.size
  }
  
  // Get grade letter
  def getGradeLetter(): String = {
    val avg = calculateOverallAverage()
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
  
  // Check if student has passed
  def hasPassed(): Boolean = calculateOverallAverage() >= 40.0
}

// Course case class
case class Course(name: String, marks: Double)

// Main application object
object StudentApp {
  // Set up logger
  private val logger = Logger.getLogger(getClass.getName)
  
  // MongoDB connection settings
  private val connectionString = "mongodb://localhost:27017"
  private val databaseName = "studentRecordSystem"
  private val collectionName = "students"
  
  // Create MongoDB client
  private def createMongoClient(): MongoClient = {
    MongoClient(connectionString)
  }
  
  // Convert Student to Document for MongoDB
  private def studentToDocument(student: Student): Document = {
    // Create individual document for each course
    val coursesArray = student.courses.map { course =>
      Document("name" -> course.name, "marks" -> course.marks)
    }
    
    // Create the main student document
    Document(
      "id" -> student.id,
      "name" -> student.name,
      "age" -> student.age,
      "courses" -> coursesArray
    )
  }
  
  // Helper methods for safely getting values from documents with proper type conversion
  private def safeGetString(doc: Document, key: String, defaultValue: String = ""): String = {
    try {
      if (doc.containsKey(key)) {
        // Convert BsonString to Scala String
        doc.getString(key).toString
      } else {
        defaultValue
      }
    } catch {
      case e: Exception =>
        logger.log(Level.WARNING, s"Error getting string value for key '$key': ${e.getMessage}")
        defaultValue
    }
  }
  
  private def safeGetDouble(doc: Document, key: String, defaultValue: Double = 0.0): Double = {
    try {
      if (doc.containsKey(key)) {
        // Convert BsonDouble to Scala Double
        doc.getDouble(key).doubleValue()
      } else {
        defaultValue
      }
    } catch {
      case e: Exception =>
        logger.log(Level.WARNING, s"Error getting double value for key '$key': ${e.getMessage}")
        defaultValue
    }
  }
  
  private def safeGetInt(doc: Document, key: String, defaultValue: Int = 0): Int = {
    try {
      if (doc.containsKey(key)) {
        // Convert BsonInt32 to Scala Int
        doc.getInteger(key).intValue()
      } else {
        defaultValue
      }
    } catch {
      case e: Exception =>
        logger.log(Level.WARNING, s"Error getting integer value for key '$key': ${e.getMessage}")
        defaultValue
    }
  }
  
  // Convert Document to Student with safer extraction
  private def documentToStudent(doc: Document): Student = {
    try {
      // Safely extract courses with proper error handling
      val coursesList = try {
        // Check if courses field exists and get it safely
        if (doc.containsKey("courses")) {
          try {
            // Get courses as a list
            val coursesList = doc.getList("courses", classOf[Document])
            // Convert each document to a Course object
            coursesList.asScala.map { courseDoc =>
              try {
                Course(
                  name = safeGetString(courseDoc, "name", "Unknown"),
                  marks = safeGetDouble(courseDoc, "marks", 0.0)
                )
              } catch {
                case e: Exception =>
                  logger.log(Level.WARNING, s"Error extracting course: ${e.getMessage}")
                  Course("Unknown", 0.0)
              }
            }.toList
          } catch {
            // If we can't get it as a list, try to get the raw value
            case _: Exception =>
              try {
                // Try getting as a raw BSON value
                val coursesValue = doc.get("courses")
                if (coursesValue.isDefined) {
                  try {
                    // If it's an array, try to process each element
                    val coursesArray = coursesValue.get
                    if (coursesArray.isArray) {
                      val values = coursesArray.asArray().getValues.asScala
                      values.map { value =>
                        try {
                          if (value.isDocument) {
                            val courseDoc = value.asDocument()
                            val name = if (courseDoc.containsKey("name")) {
                              // Convert BsonString to Scala String
                              courseDoc.getString("name").toString
                            } else "Unknown"
                            
                            val marks = if (courseDoc.containsKey("marks")) {
                              // Convert BsonDouble to Scala Double
                              courseDoc.getDouble("marks").doubleValue()
                            } else 0.0
                            
                            Course(name, marks)
                          } else {
                            Course("Unknown", 0.0)
                          }
                        } catch {
                          case e: Exception =>
                            logger.log(Level.WARNING, s"Error processing course value: ${e.getMessage}")
                            Course("Unknown", 0.0)
                        }
                      }.toList
                    } else {
                      List(Course("Unknown", 0.0))
                    }
                  } catch {
                    case e: Exception =>
                      logger.log(Level.WARNING, s"Error processing courses array: ${e.getMessage}")
                      List(Course("Unknown", 0.0))
                  }
                } else {
                  List(Course("Unknown", 0.0))
                }
              } catch {
                case e: Exception =>
                  logger.log(Level.WARNING, s"Error accessing courses: ${e.getMessage}")
                  List(Course("Unknown", 0.0))
              }
          }
        } else {
          // No courses field found
          List(Course("Unknown", 0.0))
        }
      } catch {
        case e: Exception =>
          logger.log(Level.WARNING, s"Failed to extract courses: ${e.getMessage}")
          List(Course("Unknown", 0.0))
      }
      
      // Create the student object with safely extracted values
      Student(
        id = safeGetString(doc, "id", "unknown-id"),
        name = safeGetString(doc, "name", "Unknown Student"),
        age = safeGetInt(doc, "age", 0),
        courses = coursesList
      )
    } catch {
      case e: Exception =>
        logger.log(Level.SEVERE, s"Failed to convert document to student: ${e.getMessage}")
        throw e
    }
  }
  
  // Simplified helper method for MongoDB operations
  private def handleMongoOperation[T](operation: => Observable[T], timeout: Duration = 10.seconds): Try[T] = {
    try {
      val future = operation.head()
      val result = Await.result(future, timeout)
      Success(result)
    } catch {
      case e: Exception =>
        logger.log(Level.SEVERE, s"MongoDB operation failed: ${e.getMessage}")
        Failure(e)
    }
  }
  
  // Helper method for MongoDB queries that return collections
  private def handleMongoQuery[T](query: => Observable[T], timeout: Duration = 10.seconds): Try[Seq[T]] = {
    try {
      val future = query.toFuture()
      val result = Await.result(future, timeout)
      Success(result)
    } catch {
      case e: Exception =>
        logger.log(Level.SEVERE, s"MongoDB query failed: ${e.getMessage}")
        Failure(e)
    }
  }
  
  // CRUD Operations
  
  // Create a new student record with improved error handling
  def createStudent(student: Student): Try[InsertOneResult] = {
    val mongoClient = createMongoClient()
    try {
      val database = mongoClient.getDatabase(databaseName)
      val collection = database.getCollection(collectionName)
      
      // First check if a student with this ID already exists
      val existingStudentTry = handleMongoQuery(collection.find(equal("id", student.id)))
      existingStudentTry match {
        case Success(docs) if docs.nonEmpty => 
          Failure(new Exception(s"A student with ID ${student.id} already exists"))
        case _ =>
          // Create a document with student data
          val document = studentToDocument(student)
          handleMongoOperation(collection.insertOne(document))
      }
    } catch {
      case e: Exception =>
        logger.log(Level.SEVERE, s"Failed to create student: ${e.getMessage}")
        Failure(e)
    } finally {
      mongoClient.close()
    }
  }
  
  // Read a student record by ID with simplified error handling
  def getStudentById(id: String): Try[Option[Student]] = {
    val mongoClient = createMongoClient()
    try {
      val database = mongoClient.getDatabase(databaseName)
      val collection = database.getCollection(collectionName)
      
      // Use a simpler approach with direct query
      val documentsTry = handleMongoQuery(collection.find(equal("id", id)))
      documentsTry match {
        case Success(docs) =>
          if (docs.isEmpty) {
            Success(None)
          } else {
            try {
              val doc = docs.head
              val student = documentToStudent(doc)
              Success(Some(student))
            } catch {
              case e: Exception =>
                logger.log(Level.WARNING, s"Failed to convert document to student: ${e.getMessage}")
                Success(None)
            }
          }
        case Failure(e) =>
          logger.log(Level.SEVERE, s"Failed to retrieve student with ID $id: ${e.getMessage}")
          Failure(e)
      }
    } catch {
      case e: Exception =>
        logger.log(Level.SEVERE, s"Failed to retrieve student with ID $id: ${e.getMessage}")
        Failure(e)
    } finally {
      mongoClient.close()
    }
  }
  
  // Search students by name (partial match) with simplified approach
  def searchStudentsByName(name: String): Try[Seq[Student]] = {
    val mongoClient = createMongoClient()
    try {
      val database = mongoClient.getDatabase(databaseName)
      val collection = database.getCollection(collectionName)
      
      // Create a regex pattern for partial name matching
      val pattern = s".*$name.*"
      val filter = regex("name", pattern, "i") // "i" for case-insensitive
      
      // Simplified approach to query and convert
      val documentsTry = handleMongoQuery(collection.find(filter))
      documentsTry match {
        case Success(docs) =>
          // Convert documents to students
          val students = docs.flatMap { doc =>
            try {
              Some(documentToStudent(doc))
            } catch {
              case e: Exception =>
                logger.log(Level.WARNING, s"Failed to convert document to student: ${e.getMessage}")
                None
            }
          }
          Success(students)
        case Failure(e) =>
          logger.log(Level.SEVERE, s"Failed to search students by name: ${e.getMessage}")
          Failure(e)
      }
    } catch {
      case e: Exception =>
        logger.log(Level.SEVERE, s"Failed to search students by name '$name': ${e.getMessage}")
        Failure(e)
    } finally {
      mongoClient.close()
    }
  }
  
  // Read all student records with simplified approach
  def getAllStudents(): Try[Seq[Student]] = {
    val mongoClient = createMongoClient()
    try {
      val database = mongoClient.getDatabase(databaseName)
      val collection = database.getCollection(collectionName)
      
      // Simple query to get all documents
      val documentsTry = handleMongoQuery(collection.find())
      documentsTry match {
        case Success(docs) =>
          // Convert all documents to students
          val students = docs.flatMap { doc =>
            try {
              Some(documentToStudent(doc))
            } catch {
              case e: Exception =>
                logger.log(Level.WARNING, s"Failed to convert document to student: ${e.getMessage}")
                None
            }
          }
          Success(students)
        case Failure(e) =>
          logger.log(Level.SEVERE, s"Failed to retrieve all students: ${e.getMessage}")
          Failure(e)
      }
    } catch {
      case e: Exception =>
        logger.log(Level.SEVERE, s"Failed to retrieve all students: ${e.getMessage}")
        Failure(e)
    } finally {
      mongoClient.close()
    }
  }
  
  // Update a student record with safer approach
  def updateStudent(id: String, updatedStudent: Student): Try[UpdateResult] = {
    val mongoClient = createMongoClient()
    try {
      val database = mongoClient.getDatabase(databaseName)
      val collection = database.getCollection(collectionName)
      
      // Check if student exists first
      val existingStudentTry = getStudentById(id)
      existingStudentTry match {
        case Success(Some(_)) =>
          // Student exists, perform update
          // Create a new document and use it for replacement instead of updates
          val document = studentToDocument(updatedStudent)
          
          // Use replaceOne for a more reliable update
          handleMongoOperation(collection.replaceOne(equal("id", id), document))
        case Success(None) =>
          Failure(new Exception(s"No student found with ID $id"))
        case Failure(e) =>
          Failure(e)
      }
    } catch {
      case e: Exception =>
        logger.log(Level.SEVERE, s"Failed to update student with ID $id: ${e.getMessage}")
        Failure(e)
    } finally {
      mongoClient.close()
    }
  }
  
  // Delete a student record with existence check
  def deleteStudent(id: String): Try[DeleteResult] = {
    val mongoClient = createMongoClient()
    try {
      val database = mongoClient.getDatabase(databaseName)
      val collection = database.getCollection(collectionName)
      
      // Check if student exists first
      val existingStudentTry = getStudentById(id)
      existingStudentTry match {
        case Success(Some(_)) =>
          // Student exists, perform delete
          handleMongoOperation(collection.deleteOne(equal("id", id)))
        case Success(None) =>
          Failure(new Exception(s"No student found with ID $id"))
        case Failure(e) =>
          Failure(e)
      }
    } catch {
      case e: Exception =>
        logger.log(Level.SEVERE, s"Failed to delete student with ID $id: ${e.getMessage}")
        Failure(e)
    } finally {
      mongoClient.close()
    }
  }
  
  // Report Generation
  
  // Helper method to extract proper string from potentially BsonString objects
  private def getBsonValue(value: Any): String = {
    value match {
      case s: String => s
      case bson if bson.toString.startsWith("BsonString{value='") =>
        // Extract the value from BsonString format
        val valuePattern = "BsonString\\{value='(.*?)'\\}".r
        valuePattern.findFirstMatchIn(bson.toString) match {
          case Some(m) => m.group(1)
          case None => value.toString
        }
      case _ => value.toString
    }
  }
  
  // Generate a report card for a student
  def generateReportCard(student: Student): String = {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    val currentTime = LocalDateTime.now().format(formatter)
    
    val sb = new StringBuilder()
    sb.append("=================================================\n")
    sb.append("                  REPORT CARD                    \n")
    sb.append("=================================================\n")
    sb.append(s"Date: $currentTime\n\n")
    sb.append(s"Student ID: ${student.id}\n")
    sb.append(s"Name: ${student.name}\n")
    sb.append(s"Age: ${student.age}\n\n")
    sb.append("COURSE RESULTS\n")
    sb.append("--------------------------------------------------\n")
    sb.append(f"${"Subject"}%-20s ${"Marks"}%-10s ${"Grade"}%-10s\n")
    sb.append("--------------------------------------------------\n")
    
    student.courses.foreach { course =>
      val courseName = getBsonValue(course.name)
      val gradeChar = course.marks match {
        case m if m >= 90 => "A+"
        case m if m >= 80 => "A"
        case m if m >= 70 => "B"
        case m if m >= 60 => "C"
        case m if m >= 50 => "D"
        case m if m >= 40 => "E"
        case _ => "F"
      }
      sb.append(f"${courseName}%-20s ${course.marks}%-10.1f $gradeChar%-10s\n")
    }
    
    sb.append("--------------------------------------------------\n")
    sb.append(f"${"Overall Average:"}%-20s ${student.calculateOverallAverage()}%-10.2f ${student.getGradeLetter()}%-10s\n")
    sb.append("--------------------------------------------------\n")
    sb.append(s"Result: ${if (student.hasPassed()) "PASSED" else "FAILED"}\n")
    sb.append("=================================================\n")
    
    sb.toString()
  }
  
  // Generate class report with statistics
  def generateClassReport(students: Seq[Student]): String = {
    if (students.isEmpty) {
      return "No students found to generate class report."
    }
    
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    val currentTime = LocalDateTime.now().format(formatter)
    
    val sb = new StringBuilder()
    sb.append("=================================================\n")
    sb.append("                 CLASS REPORT                    \n")
    sb.append("=================================================\n")
    sb.append(s"Date: $currentTime\n\n")
    sb.append(s"Total students: ${students.length}\n\n")
    
    // Overall statistics
    val overallAvg = students.map(_.calculateOverallAverage()).sum / students.length
    val passCount = students.count(_.hasPassed())
    val passRate = passCount.toDouble / students.length * 100
    
    sb.append("CLASS STATISTICS\n")
    sb.append("--------------------------------------------------\n")
    sb.append(f"Overall Class Average: $overallAvg%.2f\n")
    sb.append(f"Pass Rate: $passRate%.2f%% ($passCount/${students.length})\n\n")
    
    // Top 3 students
    val topStudents = students.sortBy(-_.calculateOverallAverage()).take(3)
    sb.append("TOP PERFORMING STUDENTS\n")
    sb.append("--------------------------------------------------\n")
    topStudents.zipWithIndex.foreach { case (student, index) =>
      sb.append(f"${index + 1}. ${student.name}%-20s Average: ${student.calculateOverallAverage()}%.2f (${student.getGradeLetter()})\n")
    }
    sb.append("\n")
    
    // Grade distribution
    val gradeDistribution = students.groupBy(_.getGradeLetter()).map { case (grade, students) =>
      grade -> students.length
    }
    
    sb.append("GRADE DISTRIBUTION\n")
    sb.append("--------------------------------------------------\n")
    List("A+", "A", "B", "C", "D", "E", "F").foreach { grade =>
      val count = gradeDistribution.getOrElse(grade, 0)
      val percentage = count.toDouble / students.length * 100
      sb.append(f"$grade: $count (${percentage}%.2f%%)\n")
    }
    
    sb.append("=================================================\n")
    sb.toString()
  }
  
  // Simple console menu system for interaction
  def showMenu(): Unit = {
    var running = true
    
    while (running) {
      println("\n===== STUDENT RECORD SYSTEM =====")
      println("1. Add new student")
      println("2. View all students")
      println("3. Search student by ID")
      println("4. Search students by name")
      println("5. Update student")
      println("6. Delete student")
      println("7. Generate student report card")
      println("8. Generate class report")
      println("9. Exit")
      println("=================================")
      print("Enter your choice: ")
      
      val choice = StdIn.readLine()
      
      try {
        choice match {
          case "1" => addNewStudent()
          case "2" => viewAllStudents()
          case "3" => searchStudentById()
          case "4" => searchStudentsByName()
          case "5" => updateStudent()
          case "6" => deleteStudent()
          case "7" => generateStudentReportCard()
          case "8" => generateClassReport()
          case "9" => running = false
          case _ => println("Invalid choice. Please enter a number between 1 and 9.")
        }
      } catch {
        case e: Exception =>
          println(s"Error: ${e.getMessage}")
          println("Please try again.")
      }
    }
    
    println("Thank you for using the Student Record System!")
  }
  
  // Menu functions
  
  private def addNewStudent(): Unit = {
    println("\n----- Add New Student -----")
    
    print("Enter student ID: ")
    val id = StdIn.readLine()
    
    print("Enter student name: ")
    val name = StdIn.readLine()
    
    print("Enter student age: ")
    val ageStr = StdIn.readLine()
    val age = try {
      val value = ageStr.trim.toInt
      if (value < 0 || value > 120) {
        println("Warning: Age should be between 0 and 120. Using default value 18.")
        18
      } else {
        value
      }
    } catch {
      case _: NumberFormatException => 
        println(s"Error: '$ageStr' is not a valid number. Using default value 18.")
        18
    }
    
    print("Enter number of courses (1-10): ")
    val numCoursesStr = StdIn.readLine()
    val numCourses = try {
      val value = numCoursesStr.trim.toInt
      if (value < 1) {
        println("Warning: Number of courses should be at least 1. Using default value 1.")
        1
      } else if (value > 10) {
        println("Warning: Maximum 10 courses allowed. Using value 10.")
        10
      } else {
        value
      }
    } catch {
      case _: NumberFormatException => 
        println(s"Error: '$numCoursesStr' is not a valid number. Using default value 1.")
        1
    }
    
    val courses = (1 to numCourses).map { i =>
      print(s"Enter name of course $i: ")
      val courseName = StdIn.readLine()
      
      print(s"Enter marks for $courseName (0-100): ")
      val marksStr = StdIn.readLine()
      val marks = try {
        val value = marksStr.trim.toDouble
        if (value < 0 || value > 100) {
          println(s"Warning: Marks should be between 0 and 100. Using default value.")
          0.0
        } else {
          value
        }
      } catch {
        case _: NumberFormatException => 
          println(s"Error: '$marksStr' is not a valid number. Using default value 0.0")
          0.0
      }
      
      Course(courseName, marks)
    }.toList
    
    val student = Student(id, name, age, courses)
    
    // Validate student data before saving
    if (id.trim.isEmpty) {
      println("Error: Student ID cannot be empty. Please try again.")
    } else if (name.trim.isEmpty) {
      println("Error: Student name cannot be empty. Please try again.")
    } else if (courses.isEmpty) {
      println("Error: Student must have at least one course. Please try again.")
    } else {
      createStudent(student) match {
        case Success(_) => println(s"Student $name added successfully!")
        case Failure(e) => println(s"Failed to add student: ${e.getMessage}")
      }
    }
  }
  
  private def viewAllStudents(): Unit = {
    println("\n----- All Students -----")
    
    getAllStudents() match {
      case Success(students) if students.nonEmpty =>
        println(f"${"ID"}%-10s ${"Name"}%-20s ${"Age"}%-5s ${"Avg. Marks"}%-10s ${"Grade"}%-5s")
        println("-" * 55)
        
        students.foreach { student =>
          val avg = student.calculateOverallAverage()
          val grade = student.getGradeLetter()
          println(f"${student.id}%-10s ${student.name}%-20s ${student.age}%-5d ${avg}%-10.2f $grade%-5s")
        }
        
        println(s"\nTotal: ${students.length} students")
      case Success(_) =>
        println("No students found in the system.")
      case Failure(e) =>
        println(s"Failed to retrieve students: ${e.getMessage}")
    }
  }
  
  private def searchStudentById(): Unit = {
    println("\n----- Search Student by ID -----")
    
    print("Enter student ID: ")
    val id = StdIn.readLine()
    
    getStudentById(id) match {
      case Success(Some(student)) =>
        println("\nStudent found:")
        println(f"ID: ${student.id}")
        println(f"Name: ${student.name}")
        println(f"Age: ${student.age}")
        println(f"Courses:")
        student.courses.foreach { course =>
          val courseName = getBsonValue(course.name)
          println(f"  ${courseName}%-20s ${course.marks}%.1f")
        }
        println(f"Average: ${student.calculateOverallAverage()}%.2f")
        println(f"Grade: ${student.getGradeLetter()}")
      case Success(None) =>
        println(s"No student found with ID: $id")
      case Failure(e) =>
        println(s"Failed to search for student: ${e.getMessage}")
    }
  }
  
  private def searchStudentsByName(): Unit = {
    println("\n----- Search Students by Name -----")
    
    print("Enter student name (partial search): ")
    val name = StdIn.readLine()
    
    searchStudentsByName(name) match {
      case Success(students) if students.nonEmpty =>
        println(s"\nFound ${students.length} students:")
        println(f"${"ID"}%-10s ${"Name"}%-20s ${"Age"}%-5s ${"Avg. Marks"}%-10s ${"Grade"}%-5s")
        println("-" * 55)
        
        students.foreach { student =>
          val avg = student.calculateOverallAverage()
          val grade = student.getGradeLetter()
          println(f"${student.id}%-10s ${student.name}%-20s ${student.age}%-5d ${avg}%-10.2f $grade%-5s")
        }
      case Success(_) =>
        println(s"No students found matching: $name")
      case Failure(e) =>
        println(s"Failed to search for students: ${e.getMessage}")
    }
  }
  
  private def updateStudent(): Unit = {
    println("\n----- Update Student -----")
    
    print("Enter student ID to update: ")
    val id = StdIn.readLine()
    
    getStudentById(id) match {
      case Success(Some(student)) =>
        println(s"Updating student: ${student.name}")
        
        print(s"Enter new name (current: ${student.name}): ")
        val nameInput = StdIn.readLine()
        val name = if (nameInput.trim.isEmpty) student.name else nameInput
        
        print(s"Enter new age (current: ${student.age}): ")
        val ageInput = StdIn.readLine()
        val age = if (ageInput.trim.isEmpty) student.age else {
          try {
            ageInput.trim.toInt
          } catch {
            case _: NumberFormatException => student.age
          }
        }
        
        println("Update courses? (y/n): ")
        val updateCourses = StdIn.readLine().toLowerCase.startsWith("y")
        
        val courses = if (updateCourses) {
          print("Enter number of courses: ")
          val numCoursesStr = StdIn.readLine()
          val numCourses = try {
            numCoursesStr.trim.toInt
          } catch {
            case _: NumberFormatException => 0
          }
          
          (1 to numCourses).map { i =>
            print(s"Enter name of course $i: ")
            val courseName = StdIn.readLine()
            
            print(s"Enter marks for $courseName (0-100): ")
            val marksStr = StdIn.readLine()
            val marks = try {
              val value = marksStr.trim.toDouble
              if (value < 0 || value > 100) {
                println(s"Warning: Marks should be between 0 and 100. Using default value.")
                0.0
              } else {
                value
              }
            } catch {
              case _: NumberFormatException => 
                println(s"Error: '$marksStr' is not a valid number. Using default value 0.0")
                0.0
            }
            
            Course(courseName, marks)
          }.toList
        } else {
          student.courses
        }
        
        val updatedStudent = Student(id, name, age, courses)
        
        updateStudent(id, updatedStudent) match {
          case Success(_) => println("Student updated successfully!")
          case Failure(e) => println(s"Failed to update student: ${e.getMessage}")
        }
      case Success(None) =>
        println(s"No student found with ID: $id")
      case Failure(e) =>
        println(s"Failed to search for student: ${e.getMessage}")
    }
  }
  
  private def deleteStudent(): Unit = {
    println("\n----- Delete Student -----")
    
    print("Enter student ID to delete: ")
    val id = StdIn.readLine()
    
    getStudentById(id) match {
      case Success(Some(student)) =>
        println(s"Are you sure you want to delete student: ${student.name}? (y/n): ")
        val confirm = StdIn.readLine().toLowerCase.startsWith("y")
        
        if (confirm) {
          deleteStudent(id) match {
            case Success(_) => println("Student deleted successfully!")
            case Failure(e) => println(s"Failed to delete student: ${e.getMessage}")
          }
        } else {
          println("Delete operation cancelled.")
        }
      case Success(None) =>
        println(s"No student found with ID: $id")
      case Failure(e) =>
        println(s"Failed to search for student: ${e.getMessage}")
    }
  }
  
  private def generateStudentReportCard(): Unit = {
    println("\n----- Generate Student Report Card -----")
    
    print("Enter student ID: ")
    val id = StdIn.readLine()
    
    getStudentById(id) match {
      case Success(Some(student)) =>
        val reportCard = generateReportCard(student)
        println("\nREPORT CARD GENERATED")
        println(reportCard)
        
        println("Save report to file? (y/n): ")
        val saveToFile = StdIn.readLine().toLowerCase.startsWith("y")
        
        if (saveToFile) {
          val filename = s"report_card_${student.id}_${student.name.replaceAll("\\s+", "_")}.txt"
          try {
            val pw = new java.io.PrintWriter(new java.io.File(filename))
            pw.write(reportCard)
            pw.close()
            println(s"Report card saved to $filename")
          } catch {
            case e: Exception => println(s"Failed to save report card: ${e.getMessage}")
          }
        }
      case Success(None) =>
        println(s"No student found with ID: $id")
      case Failure(e) =>
        println(s"Failed to search for student: ${e.getMessage}")
    }
  }
  
  private def generateClassReport(): Unit = {
    println("\n----- Generate Class Report -----")
    
    getAllStudents() match {
      case Success(students) if students.nonEmpty =>
        val report = generateClassReport(students)
        println("\nCLASS REPORT GENERATED")
        println(report)
        
        println("Save report to file? (y/n): ")
        val saveToFile = StdIn.readLine().toLowerCase.startsWith("y")
        
        if (saveToFile) {
          val dateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
          val filename = s"class_report_$dateTime.txt"
          try {
            val pw = new java.io.PrintWriter(new java.io.File(filename))
            pw.write(report)
            pw.close()
            println(s"Class report saved to $filename")
          } catch {
            case e: Exception => println(s"Failed to save class report: ${e.getMessage}")
          }
        }
      case Success(_) =>
        println("No students found to generate class report.")
      case Failure(e) =>
        println(s"Failed to retrieve students: ${e.getMessage}")
    }
  }
  
  // Main method to run the application
  def main(args: Array[String]): Unit = {
    // Silence all MongoDB driver logging
    Logger.getLogger("org.mongodb").setLevel(Level.SEVERE)
    Logger.getLogger("org.mongodb.driver").setLevel(Level.SEVERE)
    
    // Initialize database connection
    try {
      val mongoClient = createMongoClient()
      val database = mongoClient.getDatabase(databaseName)
      
      // Ensure collection exists
      try {
        // Check if collection exists
        val collections = Await.result(database.listCollectionNames().toFuture(), 5.seconds)
        if (!collections.contains(collectionName)) {
          // Create the collection if it doesn't exist
          Await.result(database.createCollection(collectionName).toFuture(), 5.seconds)
          println(s"Created collection: $collectionName")
        }
      } catch {
        case e: Exception => 
          // Silently continue without showing warning messages
      }
      
      // Verify connection
      val collection = database.getCollection(collectionName)
      println(s"Connected to MongoDB database: $databaseName")
      mongoClient.close()
    } catch {
      case e: Exception =>
        println(s"Error connecting to MongoDB: ${e.getMessage}")
        println("Please make sure MongoDB is running on localhost:27017")
        return
    }
    
    // Display welcome message
    println("=================================================")
    println("      WELCOME TO THE STUDENT RECORD SYSTEM       ")
    println("=================================================")
    println("This system allows you to manage student records,")
    println("search for students, and generate reports.")
    println("All data is stored in MongoDB for persistence.")
    println("=================================================")
    
    // Show the main menu
    showMenu()
  }
}

