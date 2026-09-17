dependencies {
    implementation(project(":stellar-core"))
    compileOnly("com.terraformersmc:modmenu:11.0.3-local")
    compileOnly("me.shedaniel.cloth:cloth-config-fabric:15.0.140-local")
}

tasks.named<Jar>("jar") {
    from(project(":stellar-core").the<SourceSetContainer>()["main"].output)
}
