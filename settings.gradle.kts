pluginManagement {
	repositories {
		// Shared
		mavenLocal()
		mavenCentral()
		gradlePluginPortal()
		// Stonecutter
		maven("https://maven.kikugie.dev/releases")
		maven("https://maven.kikugie.dev/snapshots")
		// JvmDowngrader
		maven("https://maven.wagyourtail.xyz/releases")
		maven("https://maven.wagyourtail.xyz/snapshots")
		// Architectury
		maven("https://maven.architectury.dev")
		// ClothConfig
		maven("https://maven.shedaniel.me")
		// Fabric
		maven("https://maven.fabricmc.net")
		// Forge
		maven("https://maven.minecraftforge.net")
		// NeoForge
		maven("https://maven.neoforged.net/releases")
		maven("https://maven.neoforged.net/snapshots")
	}
}

plugins {
    id("dev.kikugie.stonecutter") version "0.9.3"
	// This plugin will choose the necessary loom plugin conditionally.
	// Must exist in both settings.gradle.kts as well as build.gradle.kts.
    id("dev.kikugie.loom-back-compat") version "0.3"
}

stonecutter {
	kotlinController = true
	centralScript = "build.gradle.kts"

	shared {
		// Declared in preferred publishing order.

		version("fabric-26.3", "26.3")

		vcsVersion = "fabric-26.3"
	}

	create(rootProject)
}
