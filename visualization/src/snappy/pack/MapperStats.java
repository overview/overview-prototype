package snappy.pack;

public class MapperStats implements IMapperStats {

	int candidateSpriteFails		= 0;
	int candidateSpritesGenerated	= 0;
	int canvasNbrCellsGenerated		= 0;
	int canvasRectangleAddAttempts	= 0;
	
	@Override
	public int getCandidateSpriteFails() {

		return candidateSpriteFails;
	}

	@Override
	public int getCandidateSpritesGenerated() {

		return candidateSpritesGenerated;
	}

	@Override
	public int getCanvasNbrCellsGenerated() {

		return canvasNbrCellsGenerated;
	}

	@Override
	public int getCanvasRectangleAddAttempts() {

		return canvasRectangleAddAttempts;
	}

	@Override
	public void setCandidateSpriteFails(int candidateSpriteFails) {

		this.candidateSpriteFails = candidateSpriteFails;
	}

	@Override
	public void setCandidateSpritesGenerated(int candidateSpritesGenerated) {

		this.candidateSpritesGenerated = candidateSpritesGenerated;
	}

	@Override
	public void setCanvasNbrCellsGenerated(int canvasNbrCellsGenerated) {

		this.canvasNbrCellsGenerated = canvasNbrCellsGenerated;
	}

	@Override
	public void setCanvasRectangleAddAttempts(int canvasRectangleAddAttempts) {

		this.canvasRectangleAddAttempts = canvasRectangleAddAttempts;
	}

}
