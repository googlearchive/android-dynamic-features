# PlayCore API sample

This sample demonstrates usage of the PlayCore API.

Read more at http://g.co/androidappbundle

## Introduction

The sample contains several modules.

`app` -> Contains the base application which always will be installed on device.

The `MainActivity` class demonstrates how to use the API to load and launch features.

The `BaseSplitActivity` abstract class implements the required `SplitCompat.Install()` call
in the `attachBaseContext` method. This allow to launch an activity from a freshly downloaded
dynamic module without having the restart the application.

`features/*` -> Contains features which can be downloaded on demand using the PlayCore API.

Each feature as some distinctly unique characteristics.

`features/assets` -> Feature containing only assets.
`features/kotlin` -> Feature written in Kotlin.
`features/java` -> Feature written in Java.
`features/native` -> Feature written in Kotlin using JNI.

The `AndroidManifest` files in each feature show
how to declare a feature module as part of a dynamic app.


## Screenshots

<img src="screenshots/main.png" width="30%" />


## Getting Started

Clone this repository, enter the top level directory and run <code>./gradlew tasks</code>
to get an overview of all the tasks available for this project.

## Testing dynamic delivery

To test dynamic delivery with this sample, you'll need to upload it to the Google Play Store's
internal testing channel.

Before uploading, make sure to change the `applicationId` in `app/build.gradle`.


## Support

- Stack Overflow: http://stackoverflow.com/questions/tagged/android

If you've found an error *in this sample*, please file an issue:

https://github.com/googlesamples/android-dynamic-features/issues

Patches are encouraged, and may be submitted by forking this project and
submitting a pull request through GitHub.


## License

```
Copyright 2018 Google LLC.

Licensed to the Apache Software Foundation (ASF) under one or more contributor
license agreements. See the NOTICE file distributed with this work for
additional information regarding copyright ownership. The ASF licenses this
file to you under the Apache License, Version 2.0 (the "License"); you may not
use this file except in compliance with the License. You may obtain a copy of
the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
License for the specific language governing permissions and limitations under
the License.
```
