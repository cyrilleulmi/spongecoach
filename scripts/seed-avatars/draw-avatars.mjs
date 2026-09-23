#!/usr/bin/env node
/*
 * Paints the seeded Players' Avatars (ADR-0016) and writes them as a Flyway migration.
 *
 *   node scripts/seed-avatars/draw-avatars.mjs                 # (re)write the migration
 *   node scripts/seed-avatars/draw-avatars.mjs --preview <dir>  # also save each PNG + a contact sheet
 *
 * The drawing runs on a real browser canvas via Playwright (borrowed from the frontend's
 * dependencies), so the portraits come from the same 2D API as the in-app painter. They are
 * hand-drawn smileys, some bare, some with hair and doodles that nod to the Player's own skills
 * and goals. Deterministic: the same specs give the same PNGs.
 */
import { createRequire } from 'node:module';
import { mkdirSync, readFileSync, writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const here = dirname(fileURLToPath(import.meta.url));
const root = join(here, '..', '..');
const { chromium } = createRequire(join(root, 'frontend', 'package.json'))('@playwright/test');

const MIGRATION = join(root, 'backend', 'src', 'main', 'resources', 'db', 'migration', 'V12__seed_player_avatars.sql');
const MAX_BYTES = 200 * 1024;

/** Light skin tones — the team are all adult white women. */
const SKIN = { porcelain: '#fbe7da', light: '#f8dccb', peach: '#f6d0b8', warm: '#f2c6a8', rosy: '#f5cdbd' };

/**
 * One per seeded Player, keyed by the V3 seed id. Deliberately uneven: the plain ones get just
 * hair, some add a doodle, a few get the works.
 */
const PLAYERS = [
  // Plain smileys: just hair, no doodles.
  { id: '20000000-0000-0000-0000-000000000003', name: 'Gina', bg: '#f4a261', skin: SKIN.light,
    hair: { style: 'bob', color: '#c1440e' },
    eyes: 'open', mouth: 'smirk', extras: ['freckles'] },
  { id: '20000000-0000-0000-0000-000000000004', name: 'Nives', bg: '#cdb4db', skin: SKIN.porcelain,
    hair: { style: 'long', color: '#efdca0' },
    eyes: 'closed', mouth: 'smile', extras: ['blush'] },
  { id: '20000000-0000-0000-0000-000000000009', name: 'Anita', bg: '#f28482', skin: SKIN.peach,
    hair: { style: 'short', color: '#2b1d16' },
    eyes: 'open', mouth: 'grin', extras: ['fierce'] },
  { id: '20000000-0000-0000-0000-00000000000b', name: 'Sabrina', bg: '#a8dadc', skin: SKIN.rosy,
    hair: { style: 'bun', color: '#c58b4c' },
    eyes: 'happy', mouth: 'laugh', extras: ['blush'] },

  // A little hair and one doodle.
  { id: '20000000-0000-0000-0000-000000000002', name: 'Debi', bg: '#8ecae6', skin: SKIN.light,
    hair: { style: 'ponytail', color: '#e9c46a', tie: '#e63946', side: -1 }, eyes: 'open', mouth: 'o',
    extras: ['brows', 'shout'] },
  { id: '20000000-0000-0000-0000-000000000006', name: 'Sophie', bg: '#48cae4', skin: SKIN.porcelain,
    hair: { style: 'bob', color: '#f2d16b' }, eyes: 'wink', mouth: 'grin', extras: ['bolt', 'blush'] },
  { id: '20000000-0000-0000-0000-000000000007', name: 'Jana', bg: '#ff8fab', skin: SKIN.warm,
    hair: { style: 'ponytail', color: '#3b2a20', tie: '#e76f51' }, eyes: 'happy', mouth: 'tongue', extras: ['headband', 'sweat'] },
  { id: '20000000-0000-0000-0000-00000000000a', name: 'Samira', bg: '#b8e0d2', skin: SKIN.light,
    hair: { style: 'curly', color: '#5a3825' }, eyes: 'sparkle', mouth: 'smile', extras: ['star'] },
  { id: '20000000-0000-0000-0000-00000000000c', name: 'Cheyenne', bg: '#ffe066', skin: SKIN.peach,
    hair: { style: 'short', color: '#2a9d8f' }, eyes: 'wink', mouth: 'smirk', extras: ['bandaid'] },

  // The works.
  { id: '20000000-0000-0000-0000-000000000001', name: 'Carmela', bg: '#ffd166', skin: SKIN.warm,
    hair: { style: 'long', color: '#4a2c1d' }, eyes: 'open', mouth: 'grin', extras: ['lashes', 'earrings', 'stick', 'blush'] },
  { id: '20000000-0000-0000-0000-000000000005', name: 'Rahel', bg: '#90be6d', skin: SKIN.rosy,
    hair: { style: 'bun', color: '#6b4226' }, eyes: 'happy', mouth: 'laugh', glassesColor: '#e63946',
    extras: ['glasses', 'bubble', 'hearts'] },
  { id: '20000000-0000-0000-0000-000000000008', name: 'Mara', bg: '#e9edc9', skin: SKIN.light,
    hair: { style: 'long', color: '#a0522d' }, eyes: 'open', mouth: 'smirk', lips: '#b5454f',
    extras: ['sunglasses', 'earrings', 'stars', 'freckles'] },
  { id: '20000000-0000-0000-0000-00000000000d', name: 'Stocki', bg: '#a0c4ff', skin: SKIN.porcelain,
    hair: { style: 'ponytail', color: '#7a4a2a', tie: '#4c8c3d' }, eyes: 'open', mouth: 'grin',
    extras: ['cage', 'sparkles'], helmetColor: '#f4a261' },
];

function seedOf(name) {
  return [...name].reduce((hash, char) => (hash * 31 + char.charCodeAt(0)) | 0, 7);
}

async function paint() {
  const browser = await chromium.launch();
  try {
    const page = await browser.newPage();
    await page.setContent('<canvas id="c" width="512" height="512"></canvas>');
    await page.addScriptTag({ content: readFileSync(join(here, 'painter.js'), 'utf8') });
    const images = [];
    for (const player of PLAYERS) {
      const spec = { ...player, seed: seedOf(player.name) };
      const base64 = await page.evaluate((s) => {
        const canvas = document.createElement('canvas');
        canvas.width = canvas.height = 512;
        window.drawAvatar(canvas, s);
        return canvas.toDataURL('image/png').split(',')[1];
      }, spec);
      const png = Buffer.from(base64, 'base64');
      if (png.length > MAX_BYTES) {
        throw new Error(`${player.name}'s Avatar is ${png.length} bytes, over the ${MAX_BYTES} cap`);
      }
      images.push({ player, png });
    }
    return { images, page };
  } catch (error) {
    await browser.close();
    throw error;
  } finally {
    // Kept open for the contact sheet; closed by the caller.
    paint.browser = browser;
  }
}

function migration(images) {
  const rows = images
    .map(({ player, png }) => `    -- ${player.name}\n    ('${player.id}', decode('${png.toString('base64')}', 'base64'))`)
    .join(',\n');
  return `-- Painted starter Avatars for the seeded Players (ADR-0016). Generated by
-- scripts/seed-avatars/draw-avatars.mjs — edit the specs there and re-run rather than touching this file.
-- Hand-drawn smileys: some bare, some with hair and doodles (Stocki the goalie gets her mask).
-- A Player who already painted their own Avatar keeps it: the seed only fills in the missing ones.

insert into player_avatar (player_id, image) values
${rows}
on conflict (player_id) do nothing;

update player set avatar_updated_at = now()
where avatar_updated_at is null and id in (select player_id from player_avatar);
`;
}

async function contactSheet(page, images, dir) {
  const cells = images
    .map(
      ({ player, png }) =>
        `<figure><img src="data:image/png;base64,${png.toString('base64')}"><figcaption>${player.name}</figcaption></figure>`,
    )
    .join('');
  await page.setContent(`<style>
      body { margin: 0; padding: 16px; background: #f6f4ee; font: 600 15px system-ui; display: grid;
             grid-template-columns: repeat(5, 180px); gap: 14px; }
      figure { margin: 0; text-align: center; }
      img { width: 160px; height: 160px; border-radius: 50%; display: block; margin: 0 auto 6px; }
    </style>${cells}`);
  await page.setViewportSize({ width: 1000, height: 640 });
  await page.screenshot({ path: join(dir, 'contact-sheet.png'), fullPage: true });
}

const previewIndex = process.argv.indexOf('--preview');
const previewDir = previewIndex > -1 ? process.argv[previewIndex + 1] : null;

const { images, page } = await paint();
try {
  writeFileSync(MIGRATION, migration(images));
  console.log(`wrote ${MIGRATION}`);
  for (const { player, png } of images) {
    console.log(`  ${player.name.padEnd(9)} ${(png.length / 1024).toFixed(1)} KB`);
  }
  if (previewDir) {
    mkdirSync(previewDir, { recursive: true });
    for (const { player, png } of images) {
      writeFileSync(join(previewDir, `${player.name}.png`), png);
    }
    await contactSheet(page, images, previewDir);
    console.log(`previews in ${previewDir}`);
  }
} finally {
  await paint.browser.close();
}
