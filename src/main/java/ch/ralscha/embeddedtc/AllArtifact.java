package ch.ralscha.embeddedtc;

public class AllArtifact extends Artifact {

	public AllArtifact() {
		super(null, null);
	}

	@Override
	public boolean is(String groupId, String artifact) {
		return true;
	}

}
