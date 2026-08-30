# Changelog

## 1.0.0 (2026-08-30)


### Features

* add the difficulty levels and the pacing they produce ([765f2d5](https://github.com/dchernykh1984/WearOSSerpent/commit/765f2d53dc6b4304e36de392853e578f94935040))
* add the rule for what counts as a record ([06abe97](https://github.com/dchernykh1984/WearOSSerpent/commit/06abe97a716cf7b66ad35efece4fc90ec8249a30))
* add the snake rule set ([9598a38](https://github.com/dchernykh1984/WearOSSerpent/commit/9598a38b2cfe625826994730432aa9cf2871bcd6))
* cycle the difficulty on a swipe, as the original does ([123c7bc](https://github.com/dchernykh1984/WearOSSerpent/commit/123c7bca08561bbad1c172d21d47f711a9be7ef3))
* draw the board, the controls and the menus ([9155ca9](https://github.com/dchernykh1984/WearOSSerpent/commit/9155ca9a0d626a63c9a1e046d7346844f3c0aa6d))
* drive the game from a view model ([7dc5585](https://github.com/dchernykh1984/WearOSSerpent/commit/7dc55859c9ab1db0c0f60295a0223e25ac188816))
* keep the level and the records on the watch ([5a63173](https://github.com/dchernykh1984/WearOSSerpent/commit/5a631736bc78276592d42d00740df22141b7dd2c))
* lay the board and its controls out on a round screen ([417fa9c](https://github.com/dchernykh1984/WearOSSerpent/commit/417fa9c92176f9215560476a98eea8d265dc1574))
* localize the screens into eleven languages ([0086a46](https://github.com/dchernykh1984/WearOSSerpent/commit/0086a46b3c3c0a08449f073e2d1459c27f24094e))


### Bug Fixes

* announce the controls and the menu buttons as buttons ([473a127](https://github.com/dchernykh1984/WearOSSerpent/commit/473a1271610cbe2dbbe8ab4bffbb5b76d55173bf))
* **ci:** request an emulator architecture each Wear OS image exists for ([635c667](https://github.com/dchernykh1984/WearOSSerpent/commit/635c6679ad602d8ad38e6bcae26ac274490ed422))
* clear the board when the start screen comes back ([8e83502](https://github.com/dchernykh1984/WearOSSerpent/commit/8e83502a5e7d32048b99094400d547feb9486f5f))
* do not take a tick that was queued before the game was paused ([8a7f0d9](https://github.com/dchernykh1984/WearOSSerpent/commit/8a7f0d9f75f76ecee06c45a077378c0515e0b397))
* finish writing a record even if the app is closing ([df1e065](https://github.com/dchernykh1984/WearOSSerpent/commit/df1e065cb8d9de8a24d004d010ea6b33c05176c0))
* give the menu panel the corner radius it asks for, not twice it ([e527e74](https://github.com/dchernykh1984/WearOSSerpent/commit/e527e7402d77962e84a0297b9408aff031874346))
* guard the layout against a window that has no size yet ([f423e33](https://github.com/dchernykh1984/WearOSSerpent/commit/f423e33433ad267ac246fd516352e2782315aa19))
* key the control layout on every value it is worked out from ([436c450](https://github.com/dchernykh1984/WearOSSerpent/commit/436c450b7d6ba4d354c7b7fbde5fffa07e5f1ae5))
* let the game carry on when the watch cannot read or write its store ([6a7c3a2](https://github.com/dchernykh1984/WearOSSerpent/commit/6a7c3a2c84b231afb4b0e694a94292f6e7d11087))
* scale the launcher icon to the mask, which was cropping the pellet ([20e75ff](https://github.com/dchernykh1984/WearOSSerpent/commit/20e75ff4dd13dce2329948f63e5ab913511f5779))
* serialise the settings so a stored level cannot undo a tap ([41e313e](https://github.com/dchernykh1984/WearOSSerpent/commit/41e313e89f356cafdc93e243ad0b28aad32423ba))
* start the release manifest at 0.0.0 so the first release is a real 0.1.0 ([4526e7b](https://github.com/dchernykh1984/WearOSSerpent/commit/4526e7b9a2080dffb2a7e49a864a4bc847e743a9))
* stop holding the watch screen awake for a screen with no game on it ([d800107](https://github.com/dchernykh1984/WearOSSerpent/commit/d800107171d423957eb8c9809d836175c9d656bd))
