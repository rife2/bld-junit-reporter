## Compile and test the project, then display the reporter summary

```console
./bld compile test
./bld reporter
```

## Display the first group of test failures

```console
./bld reporter --index=1
```

## Display the first failure in the first group of test failures

```console
./bld reporter --i=1.2
```

## Display all failures

```console
./bld reporter --all
```
