package xyz.redsmarty.resourcepackconverter.utils;

public class InvalidResourcePackException extends Exception {
    private final String name;
    private final Reason reason;

    public InvalidResourcePackException(String name, Reason reason) {
        super(String.format("Can not convert %s, %s.", name, reason.getFormattedReason()));
        this.name = name;
        this.reason = reason;
    }

    public String getName() {
        return name;
    }

    public Reason getReason() {
        return reason;
    }

    public enum Reason {

        NO_PACK_MCMETA("could not find pack.mcmeta file"),
        INVALID_PACK_MCMETA("pack.mcmeta file is invalid"),
        OUTDATED_VERSION("resource pack is outdated, only format number 7 is supported"),
        FILE_CORRUPT("the file is corrupted");

        private final String formattedReason;

        Reason(String formattedReason) {
            this.formattedReason = formattedReason;
        }

        public String getFormattedReason() {
            return formattedReason;
        }
    }

}
