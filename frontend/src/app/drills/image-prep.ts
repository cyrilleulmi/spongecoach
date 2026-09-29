/**
 * Readies a board photo for upload (ADR-0018): upright, at most LONG_EDGE on its long side, JPEG.
 * The vision model reads nothing finer than that, and it keeps twelve photos well under the upload
 * limit.
 *
 * Phones store a portrait photo as landscape pixels plus an EXIF rotation. `createImageBitmap`
 * applies that rotation by default, so the board arrives upright; the coach's own quarter turns
 * come on top, for a photo taken sideways on purpose.
 */

export const LONG_EDGE = 1568;
const QUALITY = 0.85;

/** Scales a size down (never up) so its long edge fits. */
export function fitWithin(width: number, height: number, longEdge = LONG_EDGE): { width: number; height: number } {
  const scale = Math.min(1, longEdge / Math.max(width, height));
  return { width: Math.round(width * scale), height: Math.round(height * scale) };
}

/** The size after turning a quarter turn clockwise `turns` times. */
export function turnedSize(width: number, height: number, turns: number): { width: number; height: number } {
  return turns % 2 === 0 ? { width, height } : { width: height, height: width };
}

export function normaliseTurns(turns: number): number {
  return ((turns % 4) + 4) % 4;
}

/** Decodes (applying EXIF), turns, downscales and encodes a photo as JPEG. */
export async function prepareSketch(file: Blob, turns: number): Promise<Blob> {
  const bitmap = await createImageBitmap(file, { imageOrientation: 'from-image' });
  const quarter = normaliseTurns(turns);
  const turned = turnedSize(bitmap.width, bitmap.height, quarter);
  const target = fitWithin(turned.width, turned.height);
  const canvas = document.createElement('canvas');
  canvas.width = target.width;
  canvas.height = target.height;
  const context = canvas.getContext('2d')!;
  const scale = target.width / turned.width;
  context.translate(target.width / 2, target.height / 2);
  context.rotate((quarter * Math.PI) / 2);
  context.drawImage(bitmap, (-bitmap.width * scale) / 2, (-bitmap.height * scale) / 2, bitmap.width * scale, bitmap.height * scale);
  bitmap.close();
  return new Promise((resolve, reject) =>
    canvas.toBlob((blob) => (blob ? resolve(blob) : reject(new Error('could not encode the photo'))), 'image/jpeg', QUALITY),
  );
}
