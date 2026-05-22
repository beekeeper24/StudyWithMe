# 0023 Frontend Playwright Korean Fonts

Date: 2026-05-23

## Context

Playwright screenshots in WSL rendered Korean text as square glyphs even though the React UI itself was working. The issue was the local headless browser font environment, not the application layout.

## Decisions

- Keep Korean-capable font fallbacks in the frontend CSS stack:
  - `Noto Sans KR`
  - `Noto Sans CJK KR`
  - `Apple SD Gothic Neo`
  - `Malgun Gothic`
- If WSL does not have Korean fonts and `sudo apt install fonts-noto-cjk` is unavailable, copy Windows Korean fonts into the WSL user font directory:

```bash
mkdir -p ~/.local/share/fonts/windows-korean
cp /mnt/c/Windows/Fonts/malgun*.ttf ~/.local/share/fonts/windows-korean/
fc-cache -f ~/.local/share/fonts/windows-korean
fc-match 'sans-serif:lang=ko'
```

## Verification Rule

Before judging Korean frontend screenshots, confirm that `fc-match 'sans-serif:lang=ko'` returns a Korean-capable font such as `Malgun Gothic` or `Noto Sans CJK KR`, not `DejaVu Sans`.
