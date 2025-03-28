# Fox's Magisk Module Manager

The official Magisk is dropping support to download online modules...  
So I made my own app to do that! :3

**This app is not officially supported by Magisk or it's developers**

## Requirements

Minimum:
- Android 5.0+
- Magisk 19.0+

Recommended:
- Android 6.0+
- Magisk 21.2+

## For users

Related commits:  
- [`Remove online section in modules fragment`](https://github.com/topjohnwu/Magisk/commit/f5c982355a2e3380b2b64af4b0caa8f4f7cf9157)
- [`Cleanup unused code`](https://github.com/topjohnwu/Magisk/commit/8d59caf635591eb23813d75601039bb138f5716b)

Note: These changes didn't hit beta, or release yet, but are already live on canary builds.

The app currently use these two repo as it's module sources, with it's benefits and drawback:  
[https://github.com/Magisk-Modules-Alt-Repo](https://github.com/Magisk-Modules-Alt-Repo)  
- Accepting new modules [here](https://github.com/Magisk-Modules-Alt-Repo/submission)
- Less restrictive than the original repo
- Officially supported by Fox's mmm  

[https://github.com/Magisk-Modules-Repo](https://github.com/Magisk-Modules-Repo)  
- No longer accept new modules
- May be shut down at any moment
- Official app dropped support for it
- Officially supported by Fox's mmm

As the main repo may shutting down due to the main app no longer supporting it, 
and also stopped accepting new modules, it is recommended to submit your modules
[here](https://github.com/Magisk-Modules-Alt-Repo/submission)

If a module is in both repo, the manager will just pick the most up to date version of the module,
allowing developers to switch repo at their own pace if they want to.

## For developers

The manager can read new meta keys to allow modules to customize their own entry

It also use `minApi`, `maxApi` and `minMagisk` in the `module.prop` to detect compatibility  
And support the `support` and `donate` properties to allow them to add their own support links  
(Note: the manager use fallback values for some modules, see developer documentation for more info)

It also add new ways to control the installer ui via a new `#!` command system  
It allow module developers to have a more customizable install experience

For more information please check the [developer documentation](DEVELOPERS.md)

## Screenshots

Main activity:  
[<img src="screenshot.jpg" width="250"/>](screenshot.jpg)
