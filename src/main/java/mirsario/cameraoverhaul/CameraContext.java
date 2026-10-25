// Copyright 2020-2026 Mirsario & Contributors.
<<<<<<< HEAD
=======
// Modifications Copyright 2026 T2ws.
>>>>>>> 8f86573 (woah)
// Released under the GNU General Public License 3.0.
// See LICENSE.md for details.

package mirsario.cameraoverhaul;

import mirsario.cameraoverhaul.utilities.*;
import org.joml.*;

public class CameraContext {
	public enum Perspective {
		FIRST_PERSON,
		THIRD_PERSON,
		THIRD_PERSON_REVERSE,
	}

	public boolean isSwimming;
	public boolean isFlying;
	public boolean isSprinting;
	public boolean isRiding;
	public boolean isRidingMount;
	public boolean isRidingVehicle;
<<<<<<< HEAD
=======
	public boolean isCrouching;
	/// True when the values below come from the player model's own walk animation.
	public boolean hasWalkAnimation;
	/// The model's walk animation position. Its legs swing by cos(position * 0.6662).
	public float walkAnimationPos;
	/// The model's walk animation speed, 0 when standing still and about 1 when running.
	public float walkAnimationSpeed;
>>>>>>> 8f86573 (woah)
	public Vector3d velocity;
	public Perspective perspective;
	public Transform transform = new Transform();

	public Vector3d getForwardRelativeVelocity() {
		var temp = VectorUtils.rotate(new Vector2d(velocity.x, velocity.z), 360d - transform.eulerRot.y);
		return new Vector3d(temp.x, velocity.y, temp.y);
	}
}
