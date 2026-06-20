# TopazLifecycle Reconstruction Notes

Source material:

- `IMG_3127.MOV`: video of `contracts/TopazLifecycle.sol`, shown as 1736 lines / 1575 loc / 70.2 KB.
- `IMG_3103.JPG` to `IMG_3151.JPG`: high-resolution photos for adjacent repository files, including `TopazTypes.sol`, `TopazPayment.sol`, `TopazContacts.sol`, `TopazAccessControl.sol`, `ITopazPayment.sol`, and `scripts/deploy-topaz.js`.
- Repository visible in screenshots: `topaz-elite-solidity`, branch `dev`, internal host ending in `systems.uk.hsbc`.

Generated file:

- `contracts/TopazLifecycle.sol`

Confidence:

- High confidence: top-level imports, core record structs, event names, error names, counter/mapping layout, most external function names, and the state-machine intent.
- Medium confidence: many validation messages and helper names, because they were visible in OCR across multiple frames and are conventional with `TopazTypes`.
- Low confidence: exact line-by-line implementation, some input struct field access, exact payment request construction, and some status transitions. The video is not sharp enough to prove these one character at a time.

Important limitation:

This is not a byte-perfect reconstruction. It is a parser-valid, best-effort Solidity reconstruction guided by OCR, screenshots, and adjacent source context. The internal GitHub raw URL could not be fetched from this environment, so the original `TopazLifecycle.sol` remains the only way to guarantee exactness.

Verification performed:

- `forge fmt contracts/TopazLifecycle.sol` completed successfully, so the generated Solidity parses.
- `forge build --remappings '@openzeppelin/=/tmp/oz_stub/@openzeppelin/'` completed successfully using a minimal local `AccessControl` stub, so the generated file type-checks against the reconstructed local `TopazTypes`, `ITopazPayment`, `TopazAccessControl`, `TopazContacts`, and `TopazPayment` files.
- A normal `forge build` still requires the real OpenZeppelin dependency at `@openzeppelin/contracts/access/AccessControl.sol`.
