# 🎓 LearningFromExperience -- Java, Spring Boot, Testcontainers, GitHub Desktop & Python Debugging Course Collection

🎯 **Click any course below to enroll with my Referral Discount Code
already applied** -- save instantly on every course!

💡 **Got Udemy Business at work?** You're in luck! Most courses below
are included in your subscription -- look for the `⭐ UDEMY BUSINESS`
badge

## 🎟 Need a Coupon?

Request via
[Slack](https://join.slack.com/t/learningfromexp/shared_invite/zt-1fnksxgd0-_jOdmIq2voEeMtoindhWrA)
or email `j2eeexpert2015@gmail.com`.

------------------------------------------------------------------------

### ⭐ Featured Courses

```{=html}
<table>
```
```{=html}
<tr>
```
```{=html}
<td align="center">
```
`<a href="https://www.udemy.com/course/java-virtual-threads-structured-concurrency-with-spring-boot/?referralCode=078836F584A59839FE03">`{=html}
`<img src="https://img-c.udemycdn.com/course/480x270/6688129_bd51.jpg" width="260">`{=html}
`<br/>`{=html}`<b>`{=html}Java Virtual Threads & Structured Concurrency
with Spring Boot`</b>`{=html} `</a>`{=html}
`<br/>`{=html}`<i>`{=html}Master Virtual Threads, Structured Concurrency
& Scoped Values with Spring Boot`</i>`{=html}
`<br/>`{=html}`<br/>`{=html}
`<a href="https://www.udemy.com/course/java-virtual-threads-structured-concurrency-with-spring-boot/?referralCode=078836F584A59839FE03">`{=html}
`<img src="https://img.shields.io/badge/Udemy-Enroll%20with%20Discount-brightgreen?style=for-the-badge&logo=udemy">`{=html}
`</a>`{=html}
```{=html}
</td>
```
```{=html}
<td align="center">
```
`<a href="https://www.udemy.com/course/testcontainers-integration-testing-java-spring-boot/?referralCode=CEC06C6B9D955E06D232">`{=html}
`<img src="https://img-c.udemycdn.com/course/480x270/6525217_8d18_2.jpg" width="260">`{=html}
`<br/>`{=html}`<b>`{=html}Integration Testing with Testcontainers: Java
& Spring Boot`</b>`{=html} `</a>`{=html}
`<br/>`{=html}`<img src="https://img.shields.io/badge/⭐_UDEMY_BUSINESS-5624d0?style=flat&logoColor=white" alt="Udemy Business">`{=html}
`<br/>`{=html}`<i>`{=html}Test databases, message brokers, and APIs with
production-like containers`</i>`{=html} `<br/>`{=html}`<br/>`{=html}
`<a href="https://www.udemy.com/course/testcontainers-integration-testing-java-spring-boot/?referralCode=CEC06C6B9D955E06D232">`{=html}
`<img src="https://img.shields.io/badge/Udemy-Enroll%20with%20Discount-brightgreen?style=for-the-badge&logo=udemy">`{=html}
`</a>`{=html}
```{=html}
</td>
```
```{=html}
</tr>
```
```{=html}
</table>
```

------------------------------------------------------------------------

## ⚙️ Requirements

-   **JDK 21**
-   **Maven 3.9+**

------------------------------------------------------------------------

## 📥 Getting Started

Clone the repository:

``` bash
git clone https://github.com/j2eeexpert2015/java21-features-showcase.git && cd java21-features-showcase
```

Build the project:

``` bash
mvn clean compile
```

Create directories for logs and JFR recordings:

``` bash
mkdir -p logs jfr
```

------------------------------------------------------------------------

# ⚡ GC Comparison Demo: Retail Workload Simulation

Main class:

    org.example.concepts.zgc.RetailMemoryStress

------------------------------------------------------------------------

# PHASE 1 --- 1GB Heap (Baseline Scenario)

All collectors should run smoothly with minimal pressure.

### G1GC

``` bash
java -cp target/classes -Xmx1G -Xms1G -XX:+UseG1GC -Xlog:gc*:file=logs/g1gc-1g.log:time,level,tags -XX:StartFlightRecording=duration=60s,filename=jfr/g1gc-1g.jfr org.example.concepts.zgc.RetailMemoryStress
```

### Non-Generational ZGC

``` bash
java -cp target/classes -Xmx1G -Xms1G -XX:+UseZGC -XX:-ZGenerational -Xlog:gc*:file=logs/zgc-1g.log:time,level,tags -XX:StartFlightRecording=duration=60s,filename=jfr/zgc-1g.jfr org.example.concepts.zgc.RetailMemoryStress
```

### Generational ZGC

``` bash
java -cp target/classes -Xmx1G -Xms1G -XX:+UseZGC -XX:+ZGenerational -Xlog:gc*:file=logs/generational-zgc-1g.log:time,level,tags -XX:StartFlightRecording=duration=60s,filename=jfr/generational-zgc-1g.jfr org.example.concepts.zgc.RetailMemoryStress
```

------------------------------------------------------------------------

# PHASE 2 --- 512MB Heap (Memory Pressure Scenario)

This scenario highlights the efficiency difference between collectors.

### G1GC

``` bash
java -cp target/classes -Xmx512M -Xms512M -XX:+UseG1GC -Xlog:gc*:file=logs/g1gc-512m.log:time,level,tags -XX:StartFlightRecording=duration=60s,filename=jfr/g1gc-512m.jfr org.example.concepts.zgc.RetailMemoryStress
```

### Non-Generational ZGC

``` bash
java -cp target/classes -Xmx512M -Xms512M -XX:+UseZGC -XX:-ZGenerational -Xlog:gc*:file=logs/zgc-512m.log:time,level,tags -XX:StartFlightRecording=duration=60s,filename=jfr/zgc-512m.jfr org.example.concepts.zgc.RetailMemoryStress
```

### Generational ZGC

``` bash
java -cp target/classes -Xmx512M -Xms512M -XX:+UseZGC -XX:+ZGenerational -Xlog:gc*:file=logs/generational-zgc-512m.log:time,level,tags -XX:StartFlightRecording=duration=60s,filename=jfr/generational-zgc-512m.jfr org.example.concepts.zgc.RetailMemoryStress
```

------------------------------------------------------------------------

**Happy Learning!**
