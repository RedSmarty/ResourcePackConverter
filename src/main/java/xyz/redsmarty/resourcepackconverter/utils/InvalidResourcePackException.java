package xyz.redsmarty.resourcepackconverter.utils;

public class InvalidResourcePackException extends Exception {
    private final Reason reason;

    public InvalidResourcePackException(Reason reason) {
        super(String.format("Can not convert resource pack, %s.", reason.getFormattedReason()));
        this.reason = reason;
    }
    public Reason getReason() {
        return reason;
    }

    public enum Reason {

        NO_PACK_MCMETA("Could not find pack.mcmeta file"),
        INVALID_PACK_MCMETA("pack.mcmeta file is invalid"),
        OUTDATED_VERSION("Resource pack is outdated, only format number 7 is supported"),
        INVALID_JSON("Json syntax is invalid/not expected"),
        FILE_CORRUPT("The file is corrupted");

        private final String formattedReason;

        Reason(String formattedReason) {
            this.formattedReason = formattedReason;
        }

        public String getFormattedReason() {
            return formattedReason;
        }
    }

}
