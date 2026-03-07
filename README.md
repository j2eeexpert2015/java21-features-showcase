# 👤 About the Instructor

[![Ayan Dutta - Instructor](https://img-c.udemycdn.com/user/200_H/5007784_d6b8.jpg)](https://www.udemy.com/user/ayandutta/)

Hi, I'm **Ayan Dutta**, a Software Architect, Instructor, and Content Creator.  
I create practical, hands-on courses on **Java, Spring Boot, Debugging, Git, Python**, and more.

---

## 🌐 Connect With Me

- 💬 Slack Group: [Join Here](https://join.slack.com/t/learningfromexp/shared_invite/zt-1fnksxgd0-_jOdmIq2voEeMtoindhWrA)
- 📢 After joining, go to the **#java-virtual-threads-and-structured-concurrency** channel
- 📧 Email: j2eeexpert2015@gmail.com
- 🔗 YouTube: [LearningFromExperience](https://www.youtube.com/@learningfromexperience)
- 📝 Medium Blog: [@mrayandutta](https://medium.com/@mrayandutta)
- 💼 LinkedIn: [Ayan Dutta](https://www.linkedin.com/in/ayan-dutta-a41091b/)

---

## 📺 Subscribe on YouTube

[![YouTube](https://img.shields.io/badge/Watch%20on%20YouTube-FF0000?style=for-the-badge&logo=youtube&logoColor=white)](https://www.youtube.com/@learningfromexperience)

---

## 📚 Explore My Udemy Courses

### ⭐ Featured Courses

<table>
  <tr>
    <td align="center">
      <a href="https://www.udemy.com/course/java-virtual-threads-structured-concurrency-with-spring-boot/?referralCode=078836F584A59839FE03">
        <img src="https://img-c.udemycdn.com/course/480x270/6688129_bd51.jpg" width="260">
        <br/><b>Java Virtual Threads & Structured Concurrency with Spring Boot</b>
      </a>
      <br/><img src="https://img.shields.io/badge/⭐_UDEMY_BUSINESS-5624d0?style=flat&logoColor=white" alt="Udemy Business">
      <br/><i>Master Virtual Threads, Structured Concurrency & Scoped Values with Spring Boot</i>
      <br/><br/>
      <a href="https://www.udemy.com/course/java-virtual-threads-structured-concurrency-with-spring-boot/?referralCode=078836F584A59839FE03">
        <img src="https://img.shields.io/badge/Udemy-Enroll%20with%20Discount-brightgreen?style=for-the-badge&logo=udemy">
      </a>
    </td>
    <td align="center">
      <a href="https://www.udemy.com/course/testcontainers-integration-testing-java-spring-boot/?referralCode=CEC06C6B9D955E06D232">
        <img src="https://img-c.udemycdn.com/course/480x270/6525217_8d18_2.jpg" width="260">
        <br/><b>Integration Testing with Testcontainers: Java & Spring Boot</b>
      </a>
      <br/><img src="https://img.shields.io/badge/⭐_UDEMY_BUSINESS-5624d0?style=flat&logoColor=white" alt="Udemy Business">
      <br/><i>Test databases, message brokers, and APIs with production-like containers</i>
      <br/><br/>
      <a href="https://www.udemy.com/course/testcontainers-integration-testing-java-spring-boot/?referralCode=CEC06C6B9D955E06D232">
        <img src="https://img.shields.io/badge/Udemy-Enroll%20with%20Discount-brightgreen?style=for-the-badge&logo=udemy">
      </a>
    </td>
  </tr>
  <tr>
    <td align="center">
      <a href="https://www.udemy.com/course/intellij-idea-tips-tricks-boost-your-java-productivity/?referralCode=441FDCE063FD6C283079">
        <img src="https://img-c.udemycdn.com/course/480x270/6180669_7726.jpg" width="260">
        <br/><b>IntelliJ IDEA Tips & Tricks</b>
      </a>
      <br/><img src="https://img.shields.io/badge/⭐_UDEMY_BUSINESS-5624d0?style=flat&logoColor=white" alt="Udemy Business">
      <br/><i>Boost your Java productivity with IntelliJ IDEA hidden gems</i>
      <br/><br/>
      <a href="https://www.udemy.com/course/intellij-idea-tips-tricks-boost-your-java-productivity/?referralCode=441FDCE063FD6C283079">
        <img src="https://img.shields.io/badge/Udemy-Enroll%20with%20Discount-brightgreen?style=for-the-badge&logo=udemy">
      </a>
    </td>
    <td align="center">
      <a href="https://www.udemy.com/course/getting-started-with-github-desktop/?referralCode=B4D4C06F2EE5EF0DA450">
        <img src="https://img-c.udemycdn.com/course/480x270/6112307_3b4e_2.jpg" width="260">
        <br/><b>Complete GitHub Desktop Guide</b>
      </a>
      <br/><img src="https://img.shields.io/badge/⭐_UDEMY_BUSINESS-5624d0?style=flat&logoColor=white" alt="Udemy Business">
      <br/><i>Learn GitHub Desktop for commits, branches & collaboration</i>
      <br/><br/>
      <a href="https://www.udemy.com/course/getting-started-with-github-desktop/?referralCode=B4D4C06F2EE5EF0DA450">
        <img src="https://img.shields.io/badge/Udemy-Enroll%20with%20Discount-brightgreen?style=for-the-badge&logo=udemy">
      </a>
    </td>
  </tr>
</table>

---

## ⚙️ Requirements

- **JDK 21**
- **Maven 3.9+**

---

## 📥 Getting Started

Clone the repository:

```bash
git clone https://github.com/j2eeexpert2015/java21-features-showcase.git && cd java21-features-showcase
```

Build the project:

```bash
mvn clean compile
```

Create directories for logs and JFR recordings:

```bash
mkdir -p logs jfr
```

---

# ⚡ GC Comparison Demo: Retail Workload Simulation

Main class:

```
org.example.concepts.zgc.RetailMemoryStress
```

---

# PHASE 1 — 1GB Heap (Baseline Scenario)

All collectors should run smoothly with minimal pressure.

### G1GC

```bash
java -cp target/classes -Xmx1G -Xms1G -XX:+UseG1GC -Xlog:gc*:file=logs/g1gc-1g.log:time,level,tags -XX:StartFlightRecording=duration=60s,filename=jfr/g1gc-1g.jfr org.example.concepts.zgc.RetailMemoryStress
```

### Non-Generational ZGC

```bash
java -cp target/classes -Xmx1G -Xms1G -XX:+UseZGC -XX:-ZGenerational -Xlog:gc*:file=logs/zgc-1g.log:time,level,tags -XX:StartFlightRecording=duration=60s,filename=jfr/zgc-1g.jfr org.example.concepts.zgc.RetailMemoryStress
```

### Generational ZGC

```bash
java -cp target/classes -Xmx1G -Xms1G -XX:+UseZGC -XX:+ZGenerational -Xlog:gc*:file=logs/generational-zgc-1g.log:time,level,tags -XX:StartFlightRecording=duration=60s,filename=jfr/generational-zgc-1g.jfr org.example.concepts.zgc.RetailMemoryStress
```

---

# PHASE 2 — 512MB Heap (Memory Pressure Scenario)

This scenario highlights the efficiency difference between collectors.

### G1GC

```bash
java -cp target/classes -Xmx512M -Xms512M -XX:+UseG1GC -Xlog:gc*:file=logs/g1gc-512m.log:time,level,tags -XX:StartFlightRecording=duration=60s,filename=jfr/g1gc-512m.jfr org.example.concepts.zgc.RetailMemoryStress
```

### Non-Generational ZGC

```bash
java -cp target/classes -Xmx512M -Xms512M -XX:+UseZGC -XX:-ZGenerational -Xlog:gc*:file=logs/zgc-512m.log:time,level,tags -XX:StartFlightRecording=duration=60s,filename=jfr/zgc-512m.jfr org.example.concepts.zgc.RetailMemoryStress
```

### Generational ZGC

```bash
java -cp target/classes -Xmx512M -Xms512M -XX:+UseZGC -XX:+ZGenerational -Xlog:gc*:file=logs/generational-zgc-512m.log:time,level,tags -XX:StartFlightRecording=duration=60s,filename=jfr/generational-zgc-512m.jfr org.example.concepts.zgc.RetailMemoryStress
```

---

# 🚀 Run the Spring Boot App with Different GC Configurations

### G1GC — port 8080

```bash
java -Xmx2g -Xms2g -XX:+UseG1GC --enable-preview -jar "target/java21-features-showcase-1.0-SNAPSHOT.jar" --server.port=8080
```

### Generational ZGC — port 8081

```bash
java -Xmx2g -Xms2g -XX:+UseZGC -XX:+ZGenerational --enable-preview -jar "target/java21-features-showcase-1.0-SNAPSHOT.jar" --server.port=8081
```

### Classic ZGC — port 8082

```bash
java -Xmx2g -Xms2g -XX:+UseZGC -XX:-ZGenerational --enable-preview -jar "target/java21-features-showcase-1.0-SNAPSHOT.jar" --server.port=8082
```

---

**Happy Learning! 🚀**