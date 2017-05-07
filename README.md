# Build

```bash
$ mvn clean package
```

# Usage

```bash
$ java -jar target/fxcalculator.jar -h
Usage: fxcalculator [options]
  Options:
    -e, --execute
      Execute command and quit. Format: <ccy1> <amount1> in <ccy2> e.g AUD 100.00 in USD
      Default: []
    -h, ?, --help
      Show help
    -fp, --precisions-file
      Load in a file containing the list of currency precisions
    -q, --quiet
      hide info output for quiet mode
      Default: false
    -fr, --rates-file
      Load in a file containing the list of currency base/terms and rates
    -v, --verbose
      Write more info
      Default: false
    -V, --version
      Output version information and exit
      Default: false

```bash
$ ./fetch.sh > latest_rates.txt
$ java -jar target/fxcalculator.jar -fr latest_rates.txt
```