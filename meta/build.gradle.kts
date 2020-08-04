plugins {
  java
}

repositories {
  maven("https://jitpack.io")
  jcenter()
}

dependencies {
  val arendVersion = "v1.4.1"
  implementation("com.github.JetBrains.Arend:api:$arendVersion")
}

java {
  sourceCompatibility = JavaVersion.VERSION_11
  targetCompatibility = JavaVersion.VERSION_11
}

tasks.withType<Wrapper> {
  gradleVersion = "6.5"
}
