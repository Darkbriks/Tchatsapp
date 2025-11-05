<!-- ABOUT THE PROJECT -->
## About The Project

TchatsApp messenger service. This is the basis for a object oriented programming project in M1.

<!-- GETTING STARTED -->
## Getting Started

### Prerequisites

Java 17 or higher
Maven

### Installation

1.  Clone the repo
   ```sh
   git clone 
   ```
2. Compile/build
   ```sh
   mvn package
   ```
   
Clean the project
   ```sh
   mvn clean
   ```

<p align="right">(<a href="#readme-top">back to top</a>)</p>



<!-- USAGE EXAMPLES -->
## Usage

Launch server
   ```sh
      mvn package exec:java -Dexec.mainClass="fr.uga.im2ag.m1info.chatservice.server.TchatsAppServer"
   ```

If you obtain a "build failure" with the exception "java.net.BindException", it means that there already a server running. 

Launch client
   ```sh
      mvn package exec:java -Dexec.mainClass="fr.uga.im2ag.m1info.chatservice.client.Client"
   ```

<p align="right">(<a href="#readme-top">back to top</a>)</p>


<!-- LICENSE -->
## License

Distributed under the GPL License. See `COPYING` for more information.

<p align="right">(<a href="#readme-top">back to top</a>)</p>



<!-- CONTACT -->
## Contact


<!-- ACKNOWLEDGMENTS -->
## Acknowledgments

* []()
* []()
* []()



