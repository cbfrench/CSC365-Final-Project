# CSC 365 Final Project
*Connor French and Kelly Wong*

### Running
to run, use this in command line from this directory:

`java -cp mysql-connector-java-8.0.18/mysql-connector-java-8.0.18.jar:. InnReservations`

### Environment Variables
`export JDBC_URL=jdbc:mysql://db.labthreesixfive.com/cbfrench?autoReconnect=true\&useSSL=false`

`export JDBC_USER=cbfrench`

`export JDBC_PASS=CSC365-F2019_010176487`
### Instructions
##### FR-1
enter `pop` at the command prompt
##### FR-2
enter `book` at the command prompt, then follow prompts, currently shows a three-day interval beginning at CheckIn if initially fails to find available room
##### FR-3
enter `change` at the command prompt, then enter the reservation code, enter in details that should be changed, then hit `y` to confirm, still need to do error handling with incorrect reservation code
##### FR-4
enter `cancel` at the command prompt, then enter the reservation code, `y` to confirm and delete record, anything else fails
##### FR-5
enter 'search' at the command prompt, then enter the any combination of first name, last name, date range (must enter both start and end dates OR none), room code, and reservation code.

