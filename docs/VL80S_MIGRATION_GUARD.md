# VL80S migration single-writer guard

VL80S content migration workflows use the branch SHA as an optimistic lock.

If two generators start from the same commit, any intervening branch update invalidates
both pending pushes. A generator must re-run from the new branch head before it may
write generated content.

This prevents equipment, scheme, acceptance and diagnostic generators from racing
over the same model runtime index.
