# Use the library in Jenkins
According to [shared libraries guide](https://jenkins.io/doc/book/pipeline/shared-libraries/) you have to define shared library in Jenkins. You can define [global](https://jenkins.io/doc/book/pipeline/shared-libraries/#global-shared-libraries) shared libraries or [folder-level](https://jenkins.io/doc/book/pipeline/shared-libraries/#folder-level-shared-libraries) shared libraries.

The simpliest way is to register the library globaly and mark it to [load implicitly](https://jenkins.io/doc/book/pipeline/shared-libraries/#using-libraries).

When the library is registered as imlicitly load you can use it in your scripts. Everything that is under [`vars`](/vars) directory is accessable from a script. For components that are under [`scr`](/src) you have to use [`import`](http://groovy-lang.org/structure.html#_imports). For instance to get access to classes defined in [src/com/e4d/build](/src/com/e4d/build) you have to add the following line to the top your script.
```groovy
import com.e4d.build.*
```
### See also [how to](/README.md#howto)
- [Define a new job](/docs/howto/define-new-job.md)
- [Use sandbox job](/docs/howto/use-sandbox-job.md)
- [Use pipeline job](/docs/howto/use-pipelinejob-in-jenkinsfile.md)
