# SmartDownloader
SmartDownloader is a tool for downloading files over multiple network interfaces at the same time.

This tool uses a range-based download: each worker downloads a specific range of the target file on
a specific network interface. One worker is one connection, that means that we can specify the download speed
and how much of the file can be downloaded. When using network interfaces of different speeds we can offload more work
to one of the worker, and less work to another one. Still, SmartDownloader uses a drive download buffer,
even if you have a high bandwidth, your disk write speed may slow down the download speed.

# Usage
SmartDownloader uses a command-line interface and need to be ran in a console.
You can get the jar file either by downloading it in the releases tab in GitHub or by cloning 
the repository yourself and building the project.

The general syntax for the tool is: `java -jar SmartDownloader.jar (option) [argument] (option) [argument]...`

Available commands and parameters can be shown by using the option `-h` or `--help`

For example: `java -jar SmartDownloader.jar --help`

#### Additional commands:

| Command       | Options       | Description  |
| ------------- |:-------------:|:-----:|
| `-l, --list`  | None          | Lists the available network interfaces (with interface ID) |
| `-h, --help`  | None          | Show all available commands and options |

The default command is download, but some settings need to be supplied:
##### Download options

| Options       | Values       | Description  | Required  |
| ------------- |:------------:|:-----:|:-----:|
| `-u, --url`   | Any URL      | Specifies the url to download the target file | Yes
| `-o, --output`| None         | Lists the available network interfaces | Yes
| `-ni`         | Interface ID | Defines a new worker and assigns it to a specific network interface (all of the following options will be applied to this worker) | Yes
| `-ns`         | Byte count* | Defines the network speed at which the file will be downloaded (in bytes per seconds) | No
| `-nip`        | Float (ratio) | Specifies the part of the file downloaded by this worker | No

*Bye count: This type is a number which can have units. 

For example, these inputs will work: 

* `1024`, `1024B`, `1Kib` will be decoded as 1024 bytes per second
* `1000`, `1000B`, `1Kb` will be decoded as 1000 bytes per second
* `1000000B`, `1000000`, `8Mb` will be decoded as 1000000 bytes per second
* ... Units from Bytes to Peta Bytes (who has that much bandwidth), including SI units, are supported.

##### Integrity Check
To be sure the program downloaded fully the target file, you can provide a hash of the
final file to be compared with the hash of the newly downloaded file. Hash types supported are ones supported
by the MessageDigest Java Implementation.

| Options           | Values             | Description  | Required  |
| ----------------- |:------------------:|:----------------------------------------------|:-----:|
| `-h, --hash`      | String (hash)      | Specifies the url to download the target file | No
| `-ht, --hash-type`| String (algorithm) | Specifies what algorithm should be used to hash the file | Yes if `-h` is present

# Contribution and Issues
If you have any issue with the program let me know via the Issue Tab of the GitHub repository
Otherwise, you can contribute by creating pull requests to improve my code or to add more
features!

# License
This projects uses the Apache License, or view the [LICENSE](https://github.com/SofianeLeCubeur/SmartDownloader/blob/master/LICENSE) file.