# Use sandbox job
If you need to qickly run some script in single pre-defined pod you need to create empty pipeline job in Jenkins and put there the script below.
```groovy
sandbox {
  run {
    // Here goes your script
  }
}
```
For more details see [sandbox description](/vars/README.md#sandbox).

### See also [how to](/README.md#howto)
- [Define a new job](/docs/howto/define-new-job.md)
