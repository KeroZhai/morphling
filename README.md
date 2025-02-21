This library is currently WIP.

## Features

* [x] Basic **deep** mapping(Based on field names).
* [x] Support array or `Collection`.
* [x] Map or ignore fields conditionally.
* [ ] ...

## API Usage

```java
Source source = ...; // Get a source bean
Mapper<Source, Target> mapper = MapperFactory.getMapperFor(Source.class, Target.class);

Target target = mapper.map(source); // Deep mapping
```

Or,

```java
Source source = ...; // Get a source bean
Target target = ...; // Get an existing target bean
Mapper<Source, Target> mapper = MapperFactory.getMapperFor(Source.class, Target.class);

mapper.map(source, target); // Deep mapping
```
