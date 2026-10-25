// Copyright 2020-2026 Mirsario & Contributors.
// Modifications Copyright 2026 T2ws.
// Released under the GNU General Public License 3.0.
// See LICENSE.md for details.

package mirsario.cameraoverhaul;

import mirsario.cameraoverhaul.configuration.*;
import mirsario.cameraoverhaul.utilities.*;
import org.joml.SimplexNoise;
import org.joml.Vector2d;
import org.joml.Vector3d;

/// Physically-inspired camera motion: a gait cycle tied to travel distance, landing and jump impacts,
<<<<<<< HEAD
/// neck inertia, breathing, fatigue, and muscle tremor. Everything is rotation-only and additive.
=======
/// neck inertia, breathing, fatigue, muscle tremor, the bounce of riding a mount, flailing while falling, a heartbeat, leaning based on the feet, and following the player model's own walk animation. Everything is rotation-only and additive.
>>>>>>> 8f86573 (woah)
///
/// With bodycam mode on, the camera behaves as if strapped to the chest instead of sitting on a stabilised head:
/// every footstep jolts it, it lags behind where you look because the torso trails the head, and it has no tremor.
public final class CameraRealism {
	// Minecraft's resting vertical velocity, in blocks/tick (gravity is applied before collision resolution).
	private static final double RESTING_VERTICAL_VELOCITY = -0.0784;
	private static final double GROUNDED_VERTICAL_TOLERANCE = 0.07;
	private static final double WALK_SPEED = 4.317; // Blocks per second.
	private static final double TICK_TIME = 0.05;
<<<<<<< HEAD
=======
	private static final double TAU = Math.PI * 2.0;
	// The player model swings its legs by cos(walkPosition * 0.6662) * 1.4 * walkSpeed.
	private static final double MODEL_LEG_FREQUENCY = 0.6662;
	// The model's walk speed while walking normally (0.216 blocks per tick * 4).
	private static final double MODEL_WALK_SPEED = 0.86;
>>>>>>> 8f86573 (woah)

	// Springs. Frequencies are in Hz, damping below 1.0 means they ring after being hit.
	private final Spring landingPitch = new Spring(2.4, 0.30);
	private final Spring landingRoll = new Spring(2.0, 0.35);
	private final Spring leanPitch = new Spring(1.8, 0.50);
	private final Spring leanRoll = new Spring(1.6, 0.50);
	// Bodycam
	private final Spring stepPitch = new Spring(5.0, 0.25);
	private final Spring stepRoll = new Spring(4.0, 0.30);
	private final Spring stepYaw = new Spring(3.5, 0.35);
	private final Spring torsoYaw = new Spring(2.6, 0.85);
	private final Spring torsoPitch = new Spring(2.2, 0.85);
<<<<<<< HEAD
=======
	// Foot-based leaning
	private final Spring weightShiftRoll = new Spring(2.8, 0.55);
	private final Spring turnLeanRoll = new Spring(2.2, 0.60);
	private final Spring terrainPitch = new Spring(2.0, 0.70);
	private final Spring crouchLean = new Spring(3.0, 0.80);

	// Mount (horse) riding
	private double mountCycles; // Continuous, in full strides (two hoofbeat jolts each).
	private double mountFade;
	private long lastMountBeat;
	private double mountSide = 1.0;

	// Foot-based leaning
	private boolean leanYawValid;
	private double prevLeanYaw;
	private boolean cameraYValid;
	private double prevCameraY;

	// Falling
	private double fallAmount;

	// How strongly the footsteps shake the camera: low when walking, a bit higher when sprinting.
	private double locomotionScale = 1.0;
>>>>>>> 8f86573 (woah)

	// Gait
	private double gaitCycles; // Continuous, in full cycles (left step + right step).
	private double gaitFade;
	private double groundedFactor = 1.0;
	private long lastStepIndex;
	private double stepSide = 1.0;

	// Torso follow
	private boolean torsoInitialized;
	private double prevYaw, prevPitch;
	private double continuousYaw, continuousPitch;

	// Air state
	private boolean wasGrounded = true;
	private double fallPeak;
	private double landingSide = 1.0;

	// Tick-rate acceleration estimation
	private final Vector3d prevTickVelocity = new Vector3d();
	private final Vector3d acceleration = new Vector3d();
	private double lastVelocityChangeTime;
	private boolean initialized;

<<<<<<< HEAD
	// Fatigue / breathing
	private double fatigue;
	private double breathPhase;
=======
	// Fatigue / breathing / heart
	private double fatigue;
	private double breathPhase;
	private double heartPhase;
>>>>>>> 8f86573 (woah)

	/// Scales a value towards a bodycam-style gain. At 0 bodycam strength it stays 1, at full strength it becomes gain.
	private static double mix(double strength, double gain) {
		return 1.0 + (gain - 1.0) * strength;
	}

	public void update(CameraContext context, ConfigData.General cfg, Transform output, double deltaTime) {
		if (context.perspective != CameraContext.Perspective.FIRST_PERSON) {
			torsoInitialized = false;
<<<<<<< HEAD
=======
			leanYawValid = false;
			cameraYValid = false;
>>>>>>> 8f86573 (woah)
			return;
		}

		double time = TimeSystem.getTime();
		double dt = MathUtils.clamp(deltaTime, 0.0, 0.1);
		var velocity = context.velocity;
		double bodycam = cfg.bodycamEnabled ? MathUtils.clamp(cfg.bodycamIntensity, 0.0, 2.0) : 0.0;

		if (!initialized) {
			prevTickVelocity.set(velocity);
			lastVelocityChangeTime = time;
<<<<<<< HEAD
=======
			locomotionScale = context.isSprinting ? cfg.sprintShakeIntensity : cfg.walkShakeIntensity;
>>>>>>> 8f86573 (woah)
			initialized = true;
		}

		boolean onFoot = !context.isRiding && !context.isSwimming && !context.isFlying;
		double horizontalSpeed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z) / TICK_TIME;
		boolean groundedNow = onFoot && Math.abs(velocity.y - RESTING_VERTICAL_VELOCITY) < GROUNDED_VERTICAL_TOLERANCE;

		double rotX = 0.0, rotY = 0.0, rotZ = 0.0;

		updateFatigue(context, dt, horizontalSpeed, onFoot);
		updateAcceleration(velocity, time);
		updateAirState(velocity, groundedNow, onFoot, cfg, bodycam);

		// Smoothly blend the gait in and out so that stepping off a ledge or stopping isn't a hard cut.
		double gaitTarget = groundedNow && horizontalSpeed > 0.4 ? 1.0 : 0.0;
		gaitFade = MathUtils.damp(gaitFade, gaitTarget, 0.12, dt);
		groundedFactor = MathUtils.damp(groundedFactor, groundedNow ? 1.0 : 0.0, 0.08, dt);

		// Gait: advance by distance travelled, so the steps always match the speed of the world moving by.
<<<<<<< HEAD
		if (groundedNow && cfg.gaitCycleLength > 0.01) {
			gaitCycles += (horizontalSpeed / cfg.gaitCycleLength) * dt;
=======
		boolean modelGait = cfg.gaitFollowsModel && context.hasWalkAnimation;
		if (modelGait) {
			// Use the player model's own leg phase, so every heel strike on the camera is a heel strike on the drawn legs.
			gaitCycles = context.walkAnimationPos * MODEL_LEG_FREQUENCY / TAU;
		} else if (groundedNow && cfg.gaitCycleLength > 0.01) {
			// Real strides get longer the faster you move: short shuffling steps when slow, long bounding ones when sprinting.
			double paceRatio = MathUtils.clamp(horizontalSpeed / WALK_SPEED, 0.0, 1.6);
			double strideStretch = paceRatio < 1.0
				? 0.75 + 0.25 * paceRatio
				: 1.0 + 0.30 * MathUtils.clamp01((paceRatio - 1.0) / 0.3);
			gaitCycles += (horizontalSpeed / (cfg.gaitCycleLength * strideStretch)) * dt;
>>>>>>> 8f86573 (woah)
		}
		double gaitPhase = gaitCycles - Math.floor(gaitCycles);

		double tau = Math.PI * 2.0;
<<<<<<< HEAD
		double speedFactor = MathUtils.clamp(horizontalSpeed / WALK_SPEED, 0.0, 1.6);
		double sprintFactor = context.isSprinting ? 1.7 : 1.0;
		double fatigueFactor = 1.0 + 0.30 * fatigue * cfg.fatigueIntensity;
		double gaitAmplitude = gaitFade * speedFactor * sprintFactor * fatigueFactor * cfg.gaitIntensity;

		// Two vertical impacts per cycle (one per foot), one lateral sway per cycle. Pelvis rotation adds a touch of yaw.
		double pitchBob = Math.sin(gaitPhase * tau * 2.0) + 0.18 * Math.sin(gaitPhase * tau);
		rotX += pitchBob * 0.42 * gaitAmplitude * mix(bodycam, 2.2);
		rotZ += Math.sin(gaitPhase * tau) * 0.55 * gaitAmplitude * mix(bodycam, 1.8);
		rotY += Math.sin(gaitPhase * tau + Math.PI * 0.5) * 0.28 * gaitAmplitude * mix(bodycam, 1.5);
=======
		// With the model, the size of the leg swing sets the size of the camera motion.
		double speedFactor = modelGait
			? MathUtils.clamp(context.walkAnimationSpeed / MODEL_WALK_SPEED, 0.0, 1.6)
			: MathUtils.clamp(horizontalSpeed / WALK_SPEED, 0.0, 1.6);
		double sprintFactor = context.isSprinting ? 1.7 : 1.0;
		double fatigueFactor = 1.0 + 0.30 * fatigue * cfg.fatigueIntensity;
		// Walking shakes the camera only a little, and sprinting a bit more. Blended so that starting or stopping a sprint doesn't pop.
		locomotionScale = MathUtils.damp(locomotionScale, context.isSprinting ? cfg.sprintShakeIntensity : cfg.walkShakeIntensity, 0.15, dt);
		double gaitAmplitude = gaitFade * speedFactor * sprintFactor * fatigueFactor * cfg.gaitIntensity * locomotionScale;

		// Direction matters: moving sideways rocks the body more and bounces it less, and walking backwards is stiffer and shorter.
		var relativeVelocity = context.getForwardRelativeVelocity();
		double relativeLength = Math.sqrt(relativeVelocity.x * relativeVelocity.x + relativeVelocity.z * relativeVelocity.z);
		double lateralShare = relativeLength > 1.0e-4 ? Math.abs(relativeVelocity.x) / relativeLength : 0.0;
		double backwardShare = relativeLength > 1.0e-4 ? MathUtils.clamp01(-relativeVelocity.z / relativeLength) : 0.0;
		double directionPitch = 1.0 - 0.35 * lateralShare - 0.25 * backwardShare;
		double directionRoll = 1.0 + 0.60 * lateralShare + 0.15 * backwardShare;
		double directionYaw = 1.0 - 0.40 * lateralShare;

		// Two vertical impacts per cycle (one per foot), one lateral sway per cycle. Pelvis rotation adds a touch of yaw.
		double pitchBob = Math.sin(gaitPhase * tau * 2.0) + 0.18 * Math.sin(gaitPhase * tau);
		rotX += pitchBob * 0.42 * gaitAmplitude * directionPitch * mix(bodycam, 2.2);
		rotZ += Math.sin(gaitPhase * tau) * 0.55 * gaitAmplitude * directionRoll * mix(bodycam, 1.8);
		rotY += Math.sin(gaitPhase * tau + Math.PI * 0.5) * 0.28 * gaitAmplitude * directionYaw * mix(bodycam, 1.5);
>>>>>>> 8f86573 (woah)

		// Bodycam: each footfall slams the chest, and the camera rings out afterwards.
		long stepIndex = (long)Math.floor(gaitCycles * 2.0);
		if (stepIndex != lastStepIndex) {
			lastStepIndex = stepIndex;

			if (bodycam > 0.0 && groundedNow && gaitFade > 0.3) {
<<<<<<< HEAD
				double jolt = speedFactor * sprintFactor * fatigueFactor * cfg.bodycamStepJolt * bodycam;
=======
				double jolt = speedFactor * sprintFactor * fatigueFactor * cfg.bodycamStepJolt * bodycam * locomotionScale;
>>>>>>> 8f86573 (woah)
				stepPitch.impulse(1.3 * Math.PI * 2.0 * stepPitch.frequency * jolt);
				stepRoll.impulse(stepSide * 0.9 * Math.PI * 2.0 * stepRoll.frequency * jolt);
				stepYaw.impulse(-stepSide * 0.6 * Math.PI * 2.0 * stepYaw.frequency * jolt);
			}

			stepSide = -stepSide;
		}
		rotX += stepPitch.update(0.0, dt);
		rotZ += stepRoll.update(0.0, dt);
		rotY += stepYaw.update(0.0, dt);

<<<<<<< HEAD
=======
		// Sneaking: the player model hunches forward, so the camera pitches forward with it.
		double crouchTarget = context.isCrouching && onFoot ? 1.5 * cfg.footLeanIntensity : 0.0;
		rotX += crouchLean.update(crouchTarget, dt);

		// Leaning based on the feet. The planted foot takes the body's weight, so the camera tips towards it on every step.
		double footLean = cfg.footLeanIntensity * mix(bodycam, 1.4);
		double weightTarget = stepSide * 1.3 * gaitFade * speedFactor * (context.isSprinting ? 1.3 : 1.0) * footLean * locomotionScale;
		rotZ += weightShiftRoll.update(weightTarget, dt);

		// Running round a bend, the body leans into the turn, like a cyclist. The faster you run and the sharper you turn, the more.
		double yawNowLean = context.transform.eulerRot.y;
		if (!leanYawValid) {
			prevLeanYaw = yawNowLean;
			leanYawValid = true;
		}
		double yawStepLean = MathUtils.unwrapStep(prevLeanYaw - yawNowLean);
		prevLeanYaw = yawNowLean;
		double yawRate = dt > 1.0e-4 ? yawStepLean / dt : 0.0;
		double turnLeanTarget = groundedNow ? MathUtils.clamp(yawRate * 0.004 * speedFactor, -3.0, 3.0) * footLean : 0.0;
		rotZ += turnLeanRoll.update(turnLeanTarget, dt);

		// The ground under the feet: climbing stairs pitches you forward onto the step, going down tips you back.
		double cameraY = context.transform.position.y;
		if (!cameraYValid) {
			prevCameraY = cameraY;
			cameraYValid = true;
		}
		double riseRate = dt > 1.0e-4 ? (cameraY - prevCameraY) / dt : 0.0;
		prevCameraY = cameraY;
		boolean terrainValid = groundedNow && Math.abs(riseRate) < 12.0; // Ignore teleports and respawns.
		double terrainTarget = terrainValid ? MathUtils.clamp(riseRate * 0.9, -3.0, 3.5) * cfg.terrainLeanIntensity * mix(bodycam, 1.4) : 0.0;
		rotX += terrainPitch.update(terrainTarget, dt);

		// Riding a mount: the camera bounces to the rhythm of the hooves, and rumbles at a gallop.
		boolean mountMoving = context.isRidingMount && horizontalSpeed > 0.6;
		mountFade = MathUtils.damp(mountFade, mountMoving ? 1.0 : 0.0, 0.12, dt);
		if (mountMoving) {
			// Stride rate rises with speed, but not linearly: a galloping horse doesn't beat 10 times per second.
			mountCycles += MathUtils.clamp(0.8 + 0.22 * horizontalSpeed, 0.0, 3.2) * dt;
		}
		if (mountFade > 0.001 && cfg.mountShakeIntensity > 0.0) {
			double mountSpeedFactor = MathUtils.clamp(horizontalSpeed / 6.0, 0.0, 1.8);
			double mountAmplitude = mountFade * mountSpeedFactor * cfg.mountShakeIntensity;
			double mountPhase = mountCycles - Math.floor(mountCycles);

			// Two bounces per stride, a side-to-side rock, and a little yaw as the shoulders swing.
			rotX += Math.sin(mountPhase * tau * 2.0) * 0.55 * mountAmplitude;
			rotZ += Math.sin(mountPhase * tau) * 0.90 * mountAmplitude;
			rotY += Math.sin(mountPhase * tau + Math.PI * 0.5) * 0.30 * mountAmplitude;

			// Every hoofbeat thumps the camera and it rings out, alternating sides.
			long mountBeat = (long)Math.floor(mountCycles * 2.0);
			if (mountBeat != lastMountBeat) {
				lastMountBeat = mountBeat;
				double thump = mountAmplitude * mix(bodycam, 1.5);
				stepPitch.impulse(0.8 * Math.PI * 2.0 * stepPitch.frequency * thump);
				stepRoll.impulse(mountSide * 0.6 * Math.PI * 2.0 * stepRoll.frequency * thump);
				mountSide = -mountSide;
			}

			// Fast, fine rumble from the ground rushing past. Only noticeable when moving quickly.
			double rumble = mountAmplitude * mountAmplitude * 0.10;
			float rumbleTime = (float)(time * 22.0);
			rotX += SimplexNoise.noise(rumbleTime, 51f) * rumble;
			rotY += SimplexNoise.noise(rumbleTime, 67f) * rumble * 0.6;
			rotZ += SimplexNoise.noise(rumbleTime, 83f) * rumble * 0.8;
		} else {
			lastMountBeat = (long)Math.floor(mountCycles * 2.0);
		}

		// Falling: wind buffets the camera and the body flails, more and more the faster you drop.
		boolean fallingNow = onFoot && !groundedNow && velocity.y < -0.4;
		double fallTarget = fallingNow ? MathUtils.clamp((-velocity.y - 0.4) / 1.6, 0.0, 1.5) : 0.0;
		fallAmount = MathUtils.damp(fallAmount, fallTarget, fallingNow ? 0.10 : 0.02, dt);
		if (fallAmount > 0.001 && cfg.fallShakeIntensity > 0.0) {
			double fallScale = fallAmount * cfg.fallShakeIntensity * mix(bodycam, 1.4);
			float slow = (float)(time * 3.2);
			float fast = (float)(time * 26.0);

			// Slow, wide flailing as the body tumbles through the air.
			rotX += SimplexNoise.noise(slow, 101f) * 3.0 * fallScale;
			rotY += SimplexNoise.noise(slow, 113f) * 3.5 * fallScale;
			rotZ += SimplexNoise.noise(slow, 127f) * 4.5 * fallScale;

			// Fast buffeting from the wind rushing past, growing faster than the speed itself.
			double buffet = fallScale * fallScale * 0.55;
			rotX += SimplexNoise.noise(fast, 131f) * buffet;
			rotY += SimplexNoise.noise(fast, 149f) * buffet * 0.8;
			rotZ += SimplexNoise.noise(fast, 163f) * buffet * 1.1;
		}

>>>>>>> 8f86573 (woah)
		// Landing, jumping.
		double landingFade = onFoot ? 1.0 : 0.0;
		landingPitch.update(0.0, dt);
		landingRoll.update(0.0, dt);
		rotX += landingPitch.position * landingFade;
		rotZ += landingRoll.position * landingFade;

		// Neck inertia: the head lags behind body acceleration, then overshoots and settles.
		// On a bodycam, the chest takes the whole shove with no neck to absorb it.
		double leanScale = cfg.accelerationLeanIntensity * mix(bodycam, 1.6);
		var accelRel = VectorUtils.rotate(new Vector2d(acceleration.x, acceleration.z), 360d - context.transform.eulerRot.y);
		double leanPitchTarget = MathUtils.clamp(-accelRel.y * 33.0, -4.0, 4.0) * leanScale;
		double leanRollTarget = MathUtils.clamp(accelRel.x * 40.0, -4.0, 4.0) * leanScale;
		rotX += leanPitch.update(leanPitchTarget, dt);
		rotZ += leanRoll.update(leanRollTarget, dt);

		// Bodycam: the torso trails behind where you look, so the camera pans late and settles on target.
		if (bodycam > 0.0 && cfg.bodycamTorsoLag > 0.0) {
			double yawNow = context.transform.eulerRot.y;
			double pitchNow = context.transform.eulerRot.x;

			if (!torsoInitialized) {
				prevYaw = yawNow;
				prevPitch = pitchNow;
				continuousYaw = yawNow;
				continuousPitch = pitchNow;
				torsoYaw.reset(continuousYaw);
				torsoPitch.reset(continuousPitch);
				torsoInitialized = true;
			}

			continuousYaw += MathUtils.unwrapStep(yawNow - prevYaw);
			continuousPitch += MathUtils.unwrapStep(pitchNow - prevPitch);
			prevYaw = yawNow;
			prevPitch = pitchNow;

			double lag = cfg.bodycamTorsoLag * Math.min(bodycam, 1.0);
			// Keep the lag bounded so that what you aim at is never far from the middle of the screen.
			double yawOffset = MathUtils.clamp(torsoYaw.update(continuousYaw, dt) - continuousYaw, -25.0, 25.0);
			double pitchOffset = MathUtils.clamp(torsoPitch.update(continuousPitch, dt) - continuousPitch, -12.0, 12.0);
			rotY += yawOffset * lag;
			rotX += pitchOffset * lag * 0.6;
		} else {
			torsoInitialized = false;
		}

		// Breathing: slow when calm, fast and deep when tired. A chest-mounted camera rides it directly.
		double breathRate = 0.24 + 0.70 * fatigue;
		breathPhase += breathRate * dt;
		breathPhase -= Math.floor(breathPhase);
		double breathAmplitude = (0.07 + 0.38 * fatigue) * (horizontalSpeed > 0.4 ? 0.6 : 1.0) * mix(bodycam, 2.2);
		// Inhale is a bit quicker than exhale.
		double breath = Math.sin(breathPhase * tau + 0.35 * Math.sin(breathPhase * tau));
		rotX += breath * breathAmplitude * cfg.breathingIntensity;

<<<<<<< HEAD
=======
		// Heartbeat: a faint double thump ("lub-dub") that quickens and strengthens as you tire.
		double heartRate = 1.1 + 1.7 * fatigue;
		heartPhase += heartRate * dt;
		heartPhase -= Math.floor(heartPhase);
		double lub = Math.pow(Math.max(0.0, Math.sin(heartPhase * tau)), 6.0);
		double dub = 0.6 * Math.pow(Math.max(0.0, Math.sin((heartPhase - 0.18) * tau)), 6.0);
		double heartAmplitude = (0.025 + 0.16 * fatigue) * cfg.heartbeatIntensity * mix(bodycam, 1.8);
		rotX += (lub + dub) * heartAmplitude;

>>>>>>> 8f86573 (woah)
		// Muscle tremor: tiny, fast, never quite still. Gets worse when tired. A strapped-on camera has none of it.
		double tremorAmplitude = (0.025 + 0.07 * fatigue) * cfg.handheldTremorIntensity * mix(bodycam, 0.4);
		float tremorTime = (float)(time * 8.5);
		rotX += SimplexNoise.noise(tremorTime, 11f) * tremorAmplitude;
		rotY += SimplexNoise.noise(tremorTime, 23f) * tremorAmplitude;
		rotZ += SimplexNoise.noise(tremorTime, 37f) * tremorAmplitude * 0.5;

		output.eulerRot.x += rotX;
		output.eulerRot.y += rotY;
		output.eulerRot.z += rotZ;
	}

	private void updateFatigue(CameraContext context, double dt, double horizontalSpeed, boolean onFoot) {
		boolean exerting = onFoot && context.isSprinting && horizontalSpeed > 1.0;
		if (exerting) fatigue += dt / 9.0; // ~9s of sprinting to be properly winded.
		else fatigue -= dt / 14.0; // ~14s to recover.
		fatigue = MathUtils.clamp01(fatigue);
	}

	/// Velocity only changes on game ticks, so estimate acceleration per tick and hold it until the next one.
	private void updateAcceleration(Vector3d velocity, double time) {
		if (!velocity.equals(prevTickVelocity)) {
			acceleration.set(velocity.x - prevTickVelocity.x, 0.0, velocity.z - prevTickVelocity.z);
			prevTickVelocity.set(velocity);
			lastVelocityChangeTime = time;
		} else if (time - lastVelocityChangeTime > TICK_TIME * 1.5) {
			acceleration.set(0.0);
		}
	}

	private void updateAirState(Vector3d velocity, boolean groundedNow, boolean onFoot, ConfigData.General cfg, double bodycam) {
		if (!onFoot) {
			wasGrounded = false;
			fallPeak = 0.0;
			return;
		}

		double omegaPitch = Math.PI * 2.0 * landingPitch.frequency;

		if (wasGrounded && !groundedNow) {
			// Takeoff. A jump pushes up from the legs, so the camera kicks back a little.
			if (velocity.y > 0.25) {
				landingPitch.impulse(-1.3 * omegaPitch * cfg.jumpKickIntensity * mix(bodycam, 1.3));
			}
			fallPeak = 0.0;
		}

		if (!groundedNow) {
			fallPeak = Math.max(fallPeak, -velocity.y);
		} else if (!wasGrounded) {
			// Touchdown. The camera dips by an amount that scales with how fast we were falling, then rings out.
<<<<<<< HEAD
			double impact = MathUtils.clamp((fallPeak - 0.25) / 1.0, 0.0, 1.5);
=======
			double impact = MathUtils.clamp((fallPeak - 0.25) / 1.0, 0.0, 2.2);
>>>>>>> 8f86573 (woah)
			if (impact > 0.0) {
				double scale = impact * cfg.landingImpactIntensity * mix(bodycam, 1.5);
				landingPitch.impulse(6.0 * omegaPitch * scale);
				landingRoll.impulse(landingSide * 1.8 * Math.PI * 2.0 * landingRoll.frequency * scale);
<<<<<<< HEAD
=======
				// A hard landing also twists the camera, not just dips it.
				if (impact > 1.0) {
					stepYaw.impulse(landingSide * 1.2 * Math.PI * 2.0 * stepYaw.frequency * (impact - 1.0) * cfg.landingImpactIntensity);
				}
>>>>>>> 8f86573 (woah)
				landingSide = -landingSide;
			}
			fallPeak = 0.0;
		}

		wasGrounded = groundedNow;
	}
}
