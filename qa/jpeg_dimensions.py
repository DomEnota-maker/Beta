"""Small dependency-free JPEG SOF dimension reader for migrated source assets."""


def jpeg_dimensions(data: bytes) -> tuple[int, int]:
    if not data.startswith(b"\xff\xd8"):
        raise ValueError("missing JPEG SOI")
    position = 2
    sof_markers = {
        0xC0, 0xC1, 0xC2, 0xC3, 0xC5, 0xC6, 0xC7,
        0xC9, 0xCA, 0xCB, 0xCD, 0xCE, 0xCF,
    }
    while position < len(data):
        if data[position] != 0xFF:
            raise ValueError("invalid JPEG marker")
        while position < len(data) and data[position] == 0xFF:
            position += 1
        if position >= len(data):
            break
        marker = data[position]
        position += 1
        if marker in {0xD8, 0x01} or 0xD0 <= marker <= 0xD7:
            continue
        if marker in {0xD9, 0xDA}:
            break
        if position + 2 > len(data):
            break
        segment_size = int.from_bytes(data[position:position + 2], "big")
        if segment_size < 2 or position + segment_size > len(data):
            raise ValueError("invalid JPEG segment size")
        if marker in sof_markers:
            if segment_size < 7:
                raise ValueError("invalid JPEG SOF")
            height = int.from_bytes(data[position + 3:position + 5], "big")
            width = int.from_bytes(data[position + 5:position + 7], "big")
            if width <= 0 or height <= 0:
                raise ValueError("invalid JPEG dimensions")
            return width, height
        position += segment_size
    raise ValueError("JPEG image dimensions missing")
