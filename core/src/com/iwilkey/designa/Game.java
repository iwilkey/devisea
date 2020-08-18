package com.iwilkey.designa;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import com.iwilkey.designa.assets.Assets;
import com.iwilkey.designa.gfx.Camera;
import com.iwilkey.designa.input.InputHandler;
import com.iwilkey.designa.states.GameState;
import com.iwilkey.designa.states.State;

public class Game extends ApplicationAdapter {

	public static int w;
	public static int h;
	private GameBuffer gb;
	SpriteBatch gameBatch;
	SpriteBatch guiBatch;
	private State gameState;
	private Camera camera;
	private InputHandler input;

	@Override
	public void create () {
		w = Gdx.graphics.getWidth();
		h = Gdx.graphics.getHeight();

		Assets.init();

		gameBatch = new SpriteBatch();
		guiBatch = new SpriteBatch();

		gb = new GameBuffer(this);

		input = new InputHandler();

		gameState = new GameState(gb);

		State.setState(gameState);
		State.getCurrentState().start();
	}

	private void tick() {
		input.tick();
		if(State.getCurrentState() != null) {
			State.getCurrentState().tick();
		}
	}

	long lt = System.nanoTime();
	long now = 0;
	long timer = 0;
	int ticks = 0;
	public static int tps;

	@Override
	public void render () {

		now = System.nanoTime();
		timer += (now - lt);
		lt = now;
		ticks++;

		tick();

		Gdx.gl.glClearColor(0, 0, 0, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		gameBatch.begin();
		if(State.getCurrentState() != null) {
			State.getCurrentState().render(gameBatch);
		}
		gameBatch.setTransformMatrix(Camera.mat);
		gameBatch.end();

		guiBatch.begin();
		if(State.getCurrentState() != null) {
			State.getCurrentState().onGUI(guiBatch);
		}
		guiBatch.end();

		if(timer > 1000000000) {
			tps = ticks;
			ticks = 0;
			timer = 0;
		}
	}
	
	@Override
	public void dispose () {
		gameBatch.dispose();
		guiBatch.dispose();

		if(State.getCurrentState() != null) {
			State.getCurrentState().dispose();
		}
	}

	public Camera getCamera() {
		if(State.getCurrentState() == gameState) return camera;
		else return null;
	}

	public void setCamera(Camera camera) {
		if(State.getCurrentState() == gameState) this.camera = camera;
		else this.camera = null;
	}
}
