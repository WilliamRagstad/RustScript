# RustScript CLI (rsc)
Hello and welcome!
Thanks for installing the RustScript CLI toolchain.

## Usage

Install the CLI tool to path as shown in a chapter below. Then run:

```shell
rsc
```

This should work from anywhere.

[Read more](https://github.com/WilliamRagstad/RustScript). 

## Pre-packaged
The `.msi` installer alreay adds file associations for RustScript files, hence you can double click them
to execute them directly in explorer!

## Install to PATH
Only follow these installation steps if you have installed RustScript in `C:\Program Files\rsc`!

* Search for `env` in windows and open edit environment variables
* Click on the environment variables button
* Scroll down in the upper (or lower) variable list until you find PATH
* Click edit
* Make sure the current PATH ends with a `;`
* Paste in `C:\Program Files\rsc;`
* OK
* Done!

You should now be able to open up a new console window and type in the command `rsc` to access
the RustScript CLI from anywhere!