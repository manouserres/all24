""" This is a wrapper for network tables.
"""

import dataclasses
import ntcore
from wpimath.geometry import Transform3d
from wpiutil import wpistruct
from app.identity import Identity


@wpistruct.make_wpistruct
@dataclasses.dataclass
class Blip24:
    id: int
    pose: Transform3d


class Network:
    def __init__(self, identity: Identity) -> None:
        # TODO: use identity.name instead
        self.serial: str = identity.value
        self.inst: ntcore.NetworkTableInstance = (
            ntcore.NetworkTableInstance.getDefault()
        )
        self.inst.startClient4("tag_finder24")

        # roboRio address. windows machines can impersonate this for simulation.
        self.inst.setServer("10.1.0.2")
        topic_name: str = "vision/" + self.serial
        self.vision_capture_time_ms: ntcore.DoublePublisher = self.inst.getDoubleTopic(
            topic_name + "/capture_time_ms"
        ).publish()
        self.vision_image_age_ms = self.inst.getDoubleTopic(
            topic_name + "/image_age_ms"
        ).publish()
        self.vision_total_time_ms = self.inst.getDoubleTopic(
            topic_name + "/total_time_ms"
        ).publish()
        self.vision_detect_time_ms = self.inst.getDoubleTopic(
            topic_name + "/detect_time_ms"
        ).publish()

        # work around https://github.com/robotpy/mostrobotpy/issues/60
        self.inst.getStructTopic("bugfix", Blip24).publish().set(
            Blip24(0, Transform3d())
        )

        # blip array topic
        self.vision_nt_struct = self.inst.getStructArrayTopic(
            topic_name + "/blips", Blip24
        ).publish()

    def flush(self) -> None:
        self.inst.flush()
