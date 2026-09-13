import { normalizeParkId } from "../notifications";

describe("normalizeParkId", () => {
  it("returns unknown for missing/empty park", () => {
    expect(normalizeParkId(undefined)).toBe("unknown");
    expect(normalizeParkId("")).toBe("unknown");
    expect(normalizeParkId(42)).toBe("unknown");
  });

  it("strips hyphens to match the mobile FcmTopicManager suffix", () => {
    // Mobile replaces [\s-]+ with "_" (FcmTopicManager.kt). The functions side used to
    // keep hyphens, so park id "bwindi-impenetrable" produced different topic suffixes
    // on the two ends of the bridge and pushes never reached subscribed wardens.
    expect(normalizeParkId("bwindi-impenetrable")).toBe("bwindi_impenetrable");
    expect(normalizeParkId("Murchison Falls National Park")).toBe("murchison_falls_national_park");
  });

  it("handles camelCase and already-normalized ids", () => {
    expect(normalizeParkId("BwindiImpenetrable")).toBe("bwindi_impenetrable");
    expect(normalizeParkId("kidepo_valley")).toBe("kidepo_valley");
  });
});