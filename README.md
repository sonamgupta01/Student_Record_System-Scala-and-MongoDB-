# Student Record System

A scalable and efficient system for managing student records using Scala, Apache Spark, and MongoDB.

## Project Description

The Student Record System is a Big Data Analytics (BDA) project designed to manage, store, and analyze student records efficiently. It leverages the power of Apache Spark for data processing and analytics, MongoDB for persistent storage, and Scala as the primary programming language. The system provides basic CRUD operations for student data management and advanced analytics capabilities to derive insights from student performance data.

## Prerequisites

To run this project, you need to have the following installed:

- Java JDK 8 or later
- Scala 2.12.x
- SBT (Scala Build Tool) 1.x
- MongoDB 4.x or later
- Apache Spark 3.x

## Installation Instructions

1. **Clone the repository**
   ```bash
   git clone https://github.com/yourusername/student-record-system.git
   cd student-record-system
   ```

2. **Build the project**
   ```bash
   sbt clean compile
   ```

3. **Run the tests**
   ```bash
   sbt test
   ```

4. **Create a fat JAR (with all dependencies)**
   ```bash
   sbt assembly
   ```

## Usage Examples

### Running the Application

```bash
sbt run
```

or with the assembled JAR:

```bash
java -jar target/scala-2.12/StudentRecordSystem-assembly-0.1.0.jar
```

### Code Examples

#### Creating a new student record

```scala
import StudentApp._

val student = Student(
  id = "S001",
  name = "John Doe",
  age = 20,
  course = "Computer Science",
  grades = Map("Math" -> 85.5, "Programming" -> 92.0, "Databases" -> 88.0)
)

createStudent(student) match {
  case Success(result) => println(s"Student created successfully: ${student.name}")
  case Failure(error) => println(s"Failed to create student: ${error.getMessage}")
}
```

#### Retrieving all students

```scala
getAllStudents() match {
  case Success(students) => 
    println(s"Found ${students.length} students:")
    students.foreach(s => println(s"${s.id}: ${s.name} (${s.course})"))
  case Failure(error) => 
    println(s"Failed to retrieve students: ${error.getMessage}")
}
```

#### Analyzing student data

```scala
calculateAverageGradesByCourse() match {
  case Success(dataFrame) => 
    println("Average grades by course:")
    dataFrame.show()
  case Failure(error) => 
    println(s"Failed to calculate average grades: ${error.getMessage}")
}
```

## Features

- **Student Management**
  - Create, read, update, and delete student records
  - Store comprehensive student information including personal details and grades
  
- **Data Persistence**
  - MongoDB integration for reliable and scalable data storage
  - Error handling and transaction management
  
- **Analytics Capabilities**
  - Calculate average grades by course
  - Identify top-performing students
  - Process large datasets efficiently with Apache Spark
  
- **Scalability**
  - Designed to handle large volumes of student data
  - Optimized for performance with big data technologies
  
- **Logging and Error Handling**
  - Comprehensive logging for monitoring and debugging
  - Robust error handling for resilient operation

## Setup Instructions

### MongoDB Setup

1. **Install MongoDB**
   - Download and install MongoDB from [the official website](https://www.mongodb.com/try/download/community)
   - Follow the installation instructions for your operating system

2. **Start MongoDB server**
   ```bash
   mongod --dbpath /path/to/data/directory
   ```

3. **Create the database and collection**
   ```bash
   mongosh
   use studentRecordSystem
   db.createCollection("students")
   ```

4. **Verify the setup**
   ```bash
   show dbs
   use studentRecordSystem
   show collections
   ```

### Apache Spark Setup

1. **Download Apache Spark**
   - Download Spark from [the Apache Spark website](https://spark.apache.org/downloads.html)
   - Choose a version compatible with Scala 2.12

2. **Extract the archive**
   ```bash
   tar -xzf spark-3.3.2-bin-hadoop3.tgz
   mv spark-3.3.2-bin-hadoop3 /opt/spark
   ```

3. **Set environment variables**
   ```bash
   export SPARK_HOME=/opt/spark
   export PATH=$PATH:$SPARK_HOME/bin
   ```

4. **Verify the installation**
   ```bash
   spark-shell --version
   ```

## Troubleshooting Guide

### Common Issues and Solutions

#### MongoDB Connection Issues

- **Issue**: Unable to connect to MongoDB
- **Solution**: 
  - Ensure MongoDB server is running (`mongod` process)
  - Verify connection string in the application
  - Check MongoDB logs for errors
  - Ensure network connectivity and firewall settings allow connections

#### Spark Execution Problems

- **Issue**: Spark jobs fail to execute
- **Solution**:
  - Check if Spark is properly installed and configured
  - Ensure enough memory is allocated to Spark
  - Examine Spark UI (usually at http://localhost:4040) for detailed error information
  - Verify that the data format is compatible with the operations being performed

#### Build and Dependency Issues

- **Issue**: SBT build fails with dependency errors
- **Solution**:
  - Run `sbt clean` and then try building again
  - Check for conflicting dependencies in build.sbt
  - Ensure the correct Scala version is being used
  - Try updating SBT and its plugins

#### Data Conversion Errors

- **Issue**: Errors when converting between MongoDB documents and Scala objects
- **Solution**:
  - Ensure the document schema matches the case class structure
  - Check for type mismatches in fields
  - Verify that all required fields are present in the documents

### Getting Help

If you encounter issues not covered in this guide, you can:

1. Check the application logs for detailed error messages
2. Consult the official documentation for [MongoDB](https://docs.mongodb.com/) and [Apache Spark](https://spark.apache.org/docs/latest/)
3. Open an issue in the project's issue tracker
4. Contact the project maintainers

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Contributors

- Your Name - Initial work

## Acknowledgments

- Professor and mentors for guidance throughout the BDA course
- Open-source communities behind Scala, Spark, and MongoDB

