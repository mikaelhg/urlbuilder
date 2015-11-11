---
sectionid: set-up
sectionclass: h1
title: Use with Maven
number: 2000
---
For 2.0.5 and newer:

{% highlight xml %}
<repositories>
    <repository>
        <id>jcenter</id>
        <url>https://jcenter.bintray.com/</url>
        <snapshots>
            <enabled>false</enabled>
        </snapshots>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>io.mikael</groupId>
        <artifactId>urlbuilder</artifactId>
        <version>2.0.5</version>
    </dependency>
</dependencies>
{% endhighlight %}

For 1.3.2 and older:

{% highlight xml %}
<repositories>
    <repository>
        <id>maven2.gueck.com-releases</id>
        <url>http://maven2.gueck.com/releases</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>mikaelhg</groupId>
        <artifactId>urlbuilder</artifactId>
        <version>1.3.2</version>
    </dependency>
</dependencies>
{% endhighlight %}
