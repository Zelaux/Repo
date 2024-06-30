# Usage
```gradle
repositories{
    maven{ url 'https://raw.githubusercontent.com/Zelaux/Repo/master/repository' }
}
dependencies{
    implementaion "com.github.Author.Repo:Module:$version"
    compileOnly "com.github.Author.Repo:Module:$version"
    implementaion "com.github.Author2:Repo2:$version"
    compileOnly "com.github.Author2:Repo2:$version"
    //etc
}
```
