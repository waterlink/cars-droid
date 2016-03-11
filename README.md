# cars-droid

An example car sharing application written in Clojure for Android.

It uses [Clojure on Android toolchain](http://clojure-android.info/). It does
not try to change a way to develop Android software, rather embraces it and
adds some nice things on top of it. For example, hot code reloading, which
helps very much in development, making your feedback loop much-much shorter.
You can see update on the screen in less than a second.

To try it out, you will need:
- official Android SDK installed
- `lein` tool (http://leiningen.org/)

## Usage

### Setting up

After cloning this repository, make a symlink from your Android SDK folder to
the projects directory. For example, if your `Android` is at `~/Android`:

```bash
$ cd cars-droid
$ ln -s ~/Android/Sdk ./Sdk
```

Next step is to use Android SDK tools to download proper version of SDK and
emulator images. See info on SDK requirements from `lein-droid` side:
[SDK versions](https://github.com/clojure-android/lein-droid/wiki/Tutorial#setting-the-path-to-android-sdk).

The emulator device should be called (i.e.: its AVD is equal to) `cars`.

Then you can use `./bin/emu` script to launch your emulator. Make sure, that
your system, emulator and `qemu` supports KVM.

### Running the application

Given, that emulator is running:

```bash
$ lein droid doall
```

Additionally, this will open an `nREPL` from the emulator and expose it on
`localhost:9999`. This will allow your clojure-empowered editor to connect to
this `nREPL` and you will be able to type arbitrary code in the REPL and you
will be able to evaluate chunks of the code from your editor right inside of
the emulator inside of your application. This is what enables code-reloading.

### Providing an API server

This application requires a specific API server running. You will be able to
provide an API endpoint inside of the application (text field).

The API server can be found here: https://github.com/waterlink/cars_api

## License

Copyright © 2016 Oleksii Fedorov

Distributed under the Eclipse Public License, the same as Clojure.
