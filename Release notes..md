
When you're ready to release on Sunday, run:

```
git tag v1.0.0
git push origin v1.0.0
```

That will cause git-cliff to organize everything before that tag under `## [1.0.0]` with the release date, and anything after goes into `[unreleased]`. Clean and automatic from that point on.

That will cause git-cliff to organize everything before that tag under `## [1.0.0]` with the release date, and anything after goes into `[unreleased]`. Clean and automatic from that point on.

Going forward your commits will also look much better in the changelog once you start using conventional commit format, so instead of those long freeform messages you'll get nice grouped sections like:

```
## [unreleased]
### ✨ Features
- add Croptopia tag support
### 🐛 Bug Fixes  
- tooltip not showing for fruits bar
```


to build 
./gradlew clean build