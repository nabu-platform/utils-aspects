# Aspects

This library allows you to dynamically compose an object and to change the implementation of methods at runtime.

## The Problem

When designing a new library, I generally create a number of core interfaces and a set of optional interfaces that can extend functionality. For example the `resources` API resolves mostly around [ReadableResource](https://github.com/nablex/resources-api/blob/master/src/main/java/be/nabu/libs/resources/api/ReadableResource.java) and [ResourceContainer](https://github.com/nablex/resources-api/blob/master/src/main/java/be/nabu/libs/resources/api/ResourceContainer.java).

However these two interfaces only cover read-only systems, what if we can write as well? In come [WritableResource](https://github.com/nablex/resources-api/blob/master/src/main/java/be/nabu/libs/resources/api/WritableResource.java) and [ManageableContainer](https://github.com/nablex/resources-api/blob/master/src/main/java/be/nabu/libs/resources/api/ManageableContainer.java).

Is it a finite resource where the size is known upfront? Enter [FiniteResource](https://github.com/nablex/resources-api/blob/master/src/main/java/be/nabu/libs/resources/api/FiniteResource.java).

Does it have a timestamp of when it was last modified? Enter [TimestampedResource](https://github.com/nablex/resources-api/blob/master/src/main/java/be/nabu/libs/resources/api/TimestampedResource.java).

And the list goes on of course.

Depending on what the protocol allows for, you can implement the necessary interfaces expressing what the resource is capable of.

This is fine and all but the problem is when you start stacking implementations. Suppose you have a [file implementation](https://github.com/nablex/resources-file) of the resources api, it implements quite a number of the possible features, now suppose you want to wrap an intelligent layer around it that, when it sees a zip file, automatically exposes it using a [zip implementation](https://github.com/nablex/resources-zip) of the resources api.

In pseudocode it might look like this:

```java
FileDirectory directory = new FileItem("/path/to/directory");
assertTrue(directory instanceof ResourceContainer);
assertTrue(directory instanceof CacheableResource);

IntelligentZIPWrapper wrapper = new IntelligentZIPWrapper(directory);
assertTrue(directory instanceof ResourceContainer);
assertFalse(wrapper instanceof CacheableResource);
```

By wrapping the file directory we lose access to any specific interfaces implemented by it. As a matter of fact, the intelligent zip wrapper perhaps only wants to override the ``getChild()`` method where the directory returns a `ReadableResource` and the zip wrapper returns a `ResourceContainer`.

In essence: to override one single method, we lose access to all the other possible methods of the underlying protocol.

Additionally it is sometimes interesting to change the implementation of a method on the fly. For example there are utilities within the resources stack that allow you to take a `snapshot` of a resource system.