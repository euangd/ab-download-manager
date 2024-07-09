plugins {
    id(MyPlugins.kotlin)
    id(MyPlugins.composeBase)
}
dependencies {
    implementation(project(":downloader:core"))
    api(project(":shared:config"))
    implementation(libs.androidx.datastore)
    implementation(libs.kotlin.coroutines.core)
    implementation(libs.kotlin.serialization.json)

    implementation(compose.runtime)
}