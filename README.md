# Eclipse Store Presentation

The slides are at [SLIDES.md](SLIDES.md) and can either be viewed in a text
editor of your choosing or by using [presenterm](https://mfontanini.github.io/presenterm/introduction.html).

The example code used for demonstration purposes is located at `./src`.

The setup uses Maven and any OpenJDK 22.

You need to specify a version 22 JDK via your `~/m2/toolchains.xml`:

```
<?xml version="1.0" encoding="UTF-8"?>
<toolchains>
  <!-- JDK toolchains -->
  <toolchain>
    <type>jdk</type>
    <provides>
      <version>22</version>
      <vendor>openjdk</vendor>
    </provides>
    <configuration>
      <jdkHome>/path/to/jdk/17</jdkHome>
    </configuration>
  </toolchain>
</toolchains>`
```

Code can be executed like this:

```
mvn -q clean compile exec:exec
```

You are free to whatever the hell you want with the code and presentation :)
