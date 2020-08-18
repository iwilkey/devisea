package com.iwilkey.designa.desktop;

import com.badlogic.gdx.Files;
import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;

import com.iwilkey.designa.Game;

public class DesktopLauncher {
	public static void main (String[] arg) {

		LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();

		config.title = "Designa";
		config.width = 1920;
		config.height = 1280;
		config.resizable = false;
		config.fullscreen = true;
		config.addIcon("icon.png", Files.FileType.Internal);

		new LwjglApplication(new Game(), config);

	}
}
