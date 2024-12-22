# pylint: disable=C0103,E0611,E1101,R0402


import unittest

import gtsam
import numpy as np
from gtsam import noiseModel  # type:ignore
from gtsam.symbol_shorthand import X  # type:ignore

import app.pose_estimator.factors.apriltag_smooth_batch as apriltag_smooth_batch

KCAL = gtsam.Cal3DS2(200.0, 200.0, 0.0, 200.0, 200.0, -0.2, 0.1)
NOISE2 = noiseModel.Diagonal.Sigmas(np.array([0.1, 0.1]))


class AprilTagSmoothBatchTest(unittest.TestCase):

    def test_h_center_1(self) -> None:
        landmarks = [np.array([1, 0, 0]), np.array([1, 0, 0])]
        p0 = gtsam.Pose2()
        offset = gtsam.Pose3()
        estimate_px: np.ndarray = apriltag_smooth_batch.h_fn(landmarks, offset, KCAL)(
            p0
        )
        print("estimate: ", estimate_px)
        # landmark on the camera bore, so it's at (cx, cy)
        self.assertTrue(
            np.allclose(
                np.array(
                    [
                        200,
                        200,
                        200,
                        200,
                    ]
                ),
                estimate_px,
            )
        )

    def test_h_side_0(self) -> None:
        # second point is above the camera bore
        landmarks = [np.array([1, 0, 0]), np.array([1, 0, 1])]
        p0 = gtsam.Pose2()
        offset = gtsam.Pose3()
        estimate_px: np.ndarray = apriltag_smooth_batch.h_fn(landmarks, offset, KCAL)(
            p0
        )
        # landmark above the camera bore, so the 'y' value is less
        self.assertTrue(
            np.allclose(
                np.array(
                    [
                        200,
                        200,
                        200,
                        20,
                    ]
                ),
                estimate_px,
            )
        )

    def test_h_upper_left(self) -> None:
        # second point is above and to the left
        landmarks = [np.array([1, 0, 0]), np.array([1, 1, 1])]
        p0 = gtsam.Pose2()
        offset = gtsam.Pose3()
        estimate_px: np.ndarray = apriltag_smooth_batch.h_fn(landmarks, offset, KCAL)(
            p0
        )
        # above and to the left, so both x and y are less.
        # (coincidentally right on the edge)
        print("estimate: ", estimate_px)
        self.assertTrue(
            np.allclose(
                np.array(
                    [
                        200,
                        200,
                        0,
                        0,
                    ]
                ),
                estimate_px,
            )
        )

    def test_H_upper_left(self) -> None:
        # as above but with jacobians
        measured = np.array([200, 200, 0, 0])
        landmarks = [np.array([1, 0, 0]), np.array([1, 1, 1])]
        p0 = gtsam.Pose2()
        offset = gtsam.Pose3()
        H = [np.zeros((3, 2))]
        err_px: np.ndarray = apriltag_smooth_batch.h_H(
            landmarks, measured, p0, offset, KCAL, H
        )
        # same case as above
        print("err_px", err_px)
        self.assertTrue(
            np.allclose(
                np.array(
                    [
                        0,
                        0,
                        0,
                        0,
                    ]
                ),
                err_px,
            )
        )
        print("H[0]:\n", H[0])
        self.assertTrue(
            np.allclose(
                np.array(
                    [
                        [0, 200, 200],
                        [0, 0.0, 0],
                        [-360, 280, 640],
                        [-360, 80, 440],
                    ]
                ),
                H[0],
                atol=0.001,
            )
        )

    def test_factor(self) -> None:
        landmarks = [
            np.array([1, 0, 0]),  # on bore, 1m away (in x)
            np.array([1, 1, 1]),  # upper left corner
        ]
        measured = np.array([200, 200, 0, 0])
        offset = gtsam.Pose3()
        f: gtsam.NoiseModelFactor = apriltag_smooth_batch.factor(
            landmarks,
            measured,
            offset,
            KCAL,
            NOISE2,
            X(0),
        )
        v = gtsam.Values()
        p0 = gtsam.Pose2()
        v.insert(X(0), p0)
        err_px = f.unwhitenedError(v)
        print("err: ", err_px)
        self.assertTrue(
            np.allclose(
                np.array(
                    [
                        0,
                        0,
                        0,
                        0,
                    ]
                ),
                err_px,
            )
        )

    def test_factor2(self) -> None:
        landmarks = [
            np.array([1, 0, 0]),  # on bore, 1m away (in x)
            np.array([1, 1, 1]),  # upper left corner
        ]
        # camera offset up, so measured points
        # should be down
        measured = np.array(
            [
                200,  # center
                380,  # lower
                20,  # distorted
                200,  # center
            ]
        )
        offset = gtsam.Pose3(gtsam.Rot3(), np.array([0, 0, 1]))
        f: gtsam.NoiseModelFactor = apriltag_smooth_batch.factor(
            landmarks,
            measured,
            offset,
            KCAL,
            NOISE2,
            X(0),
        )
        v = gtsam.Values()
        p0 = gtsam.Pose2()
        v.insert(X(0), p0)
        err_px = f.unwhitenedError(v)
        print("err: ", err_px)
        self.assertTrue(
            np.allclose(
                np.array(
                    [
                        0,
                        0,
                        0,
                        0,
                    ]
                ),
                err_px,
            )
        )

    def test_factor3(self) -> None:
        landmarks = [
            np.array([1, 0, 0]),  # on bore, 1m away (in x)
            np.array([1, 1, 1]),  # upper left corner
        ]
        # camera tilt up, so measured points
        # should be down
        measured = np.array(
            [
                200,  # center
                220,  # bore is slightly above target
                31,  # distorted
                49,
            ]
        )
        offset = gtsam.Pose3(gtsam.Rot3.Pitch(-0.1), np.array([0, 0, 0]))
        f: gtsam.NoiseModelFactor = apriltag_smooth_batch.factor(
            landmarks,
            measured,
            offset,
            KCAL,
            NOISE2,
            X(0),
        )
        v = gtsam.Values()
        p0 = gtsam.Pose2()
        v.insert(X(0), p0)
        err_px = f.unwhitenedError(v)
        print("err: ", err_px)
        self.assertTrue(
            np.allclose(
                np.array(
                    [
                        0,
                        0,
                        0,
                        0,
                    ]
                ),
                err_px,
                atol=1,
            )
        )

    def test_factor4(self) -> None:
        landmarks = [
            np.array([1, 0, 0]),  # on bore, 1m away (in x)
            np.array([1, 1, 1]),  # upper left corner
        ]
        # closer, so points are closer to the edges
        measured = np.array(
            [
                200,  # center
                390,  # closer to the edge
                10,  # closer to the edge
                200,  # center
            ]
        )
        offset = gtsam.Pose3(gtsam.Rot3(), np.array([0, 0, 1]))
        f: gtsam.NoiseModelFactor = apriltag_smooth_batch.factor(
            landmarks,
            measured,
            offset,
            KCAL,
            NOISE2,
            X(0),
        )
        v = gtsam.Values()
        p0 = gtsam.Pose2(0.05, 0, 0)
        v.insert(X(0), p0)
        err_px = f.unwhitenedError(v)
        print("err: ", err_px)
        self.assertTrue(
            np.allclose(
                np.array(
                    [
                        0,
                        0,
                        0,
                        0,
                    ]
                ),
                err_px,
                atol=1,
            )
        )
