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

### 🧩 Java Debugging Courses with Eclipse, IntelliJ IDEA, and VS Code

<table>
  <tr>
    <td>
      <a href="https://www.udemy.com/course/eclipse-debugging-techniques-and-tricks">
        <img src="https://img-c.udemycdn.com/course/480x270/417118_3afa_4.jpg" width="250"><br/>
        <b>Eclipse Debugging Techniques</b>
      </a>
    </td>
    <td>
      <a href="https://www.udemy.com/course/java-debugging-with-intellij-idea">
        <img src="https://img-c.udemycdn.com/course/480x270/2608314_47e4.jpg" width="250"><br/>
        <b>Java Debugging With IntelliJ</b>
      </a>
    </td>
    <td>
      <a href="https://www.udemy.com/course/java-debugging-with-visual-studio-code-the-ultimate-guide">
        <img src="https://img-c.udemycdn.com/course/480x270/5029852_d692_3.jpg" width="250"><br/>
        <b>Java Debugging with VS Code</b>
      </a>
    </td>
  </tr>
</table>

---

### 💡 Java Productivity & Patterns

<table>
  <tr>
    <td>
      <a href="https://www.udemy.com/course/intellij-idea-tips-tricks-boost-your-java-productivity">
        <img src="https://img-c.udemycdn.com/course/480x270/6180669_7726.jpg" width="250"><br/>
        <b>IntelliJ IDEA Tips & Tricks</b>
      </a>
    </td>
    <td>
      <a href="https://www.udemy.com/course/design-patterns-in-javacreational">
        <img src="https://img-c.udemycdn.com/course/480x270/779796_5770_2.jpg" width="250"><br/>
        <b>Creational Design Patterns</b>
      </a>
    </td>
  </tr>
</table>

---

### 🐍 Python Debugging Courses

<table>
  <tr>
    <td>
      <a href="https://www.udemy.com/course/learn-python-debugging-with-pycharm-ide">
        <img src="https://img-c.udemycdn.com/course/480x270/4840890_12a3_2.jpg" width="250"><br/>
        <b>Python Debugging With PyCharm</b>
      </a>
    </td>
    <td>
      <a href="https://www.udemy.com/course/python-debugging-with-visual-studio-code">
        <img src="https://img-c.udemycdn.com/course/480x270/5029842_d36f.jpg" width="250"><br/>
        <b>Python Debugging with VS Code</b>
      </a>
    </td>
    <td>
      <a href="https://www.udemy.com/course/get-started-with-python-debugging-in-visual-studio-code">
        <img src="https://img-c.udemycdn.com/course/480x270/6412275_a17d.jpg" width="250"><br/>
        <b>Python Debugging (Free)</b>
      </a>
    </td>
  </tr>
</table>

---

### 🛠 Git & GitHub Courses

<table>
  <tr>
    <td>
      <a href="https://www.udemy.com/course/getting-started-with-github-desktop">
        <img src="https://img-c.udemycdn.com/course/480x270/6112307_3b4e_2.jpg" width="250"><br/>
        <b>GitHub Desktop Guide</b>
      </a>
    </td>
    <td>
      <a href="https://www.udemy.com/course/learn-to-use-git-and-github-with-eclipse-a-complete-guide">
        <img src="https://img-c.udemycdn.com/course/480x270/3369428_995b.jpg" width="250"><br/>
        <b>Git & GitHub with Eclipse</b>
      </a>
    </td>
  </tr>
</table>


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

**Happy Learning! 🚀**