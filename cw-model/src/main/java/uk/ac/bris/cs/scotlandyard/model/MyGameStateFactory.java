package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import uk.ac.bris.cs.scotlandyard.model.Board.GameState;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Factory;

import java.util.*;
import java.util.function.BiConsumer;

/**
 * cw-model
 * Stage 1: Complete this class
 */
public final class MyGameStateFactory implements Factory<GameState> {

	@Nonnull @Override public GameState build(
			GameSetup setup,
			Player mrX,
			ImmutableList<Player> detectives) {
            return new MyGameState(setup, ImmutableSet.of(Piece.MrX.MRX), ImmutableList.of(), mrX, detectives, 1);

	}
	     private static ImmutableSet<Move.SingleMove> makeSingleMoves(GameSetup setup, List<Player> detectives, Player player, int source){
             final List<Move.SingleMove> singleMoves = new ArrayList<>();
             for (int destination : setup.graph.adjacentNodes(source)){
             	var occupied = false;
             	for (Player p : detectives){
             		if (p.location() == destination) occupied = true;
				}
             	if(occupied) continue;
             	for(ScotlandYard.Transport t : setup.graph.edgeValueOrDefault(source,destination, ImmutableSet.of())){
             		if(player.has(t.requiredTicket())) singleMoves.add(new Move.SingleMove(player.piece(),source,t.requiredTicket(),destination));
				}
             	if(player.has(ScotlandYard.Ticket.SECRET)) singleMoves.add(new Move.SingleMove(player.piece(),source, ScotlandYard.Ticket.SECRET,destination));

			 }
             return ImmutableSet.copyOf(singleMoves);
	     }
	     private static ImmutableSet<Move.DoubleMove> makeDoubleMoves(GameSetup setup, List<Player> detectives, Player mrX, int source){
			 final List<Move.DoubleMove> doubleMoves = new ArrayList<>();
			 Map<ScotlandYard.Ticket,Integer> Copyofplayerticket = new HashMap<>();
		     if (mrX.has(ScotlandYard.Ticket.DOUBLE) && (setup.rounds.size() >= 2)){  //If X has double ticket and still enough rounds left to make a double move
		     for (int destination1 : setup.graph.adjacentNodes(source)){ //All the adjacentNodes to source
		     	var occupied = false;
		     	for (Player p : detectives){                                //Check if the destination is occupied by a detective.
					if(p.location() == destination1) occupied = true;       // If it is then the move should not be available and
				}	                                                        //should move onto checking for the next potential destination
		     	if(occupied) continue;

		     	for (ScotlandYard.Transport t : setup.graph.edgeValueOrDefault(source, destination1, ImmutableSet.of())) { //All the transports from source to destination1
					Copyofplayerticket.clear();
					Copyofplayerticket.putAll(mrX.tickets());    // Make a muttable copy of player ticket so we can subtract 1 everytime ticket can be used for first move.
					if(mrX.has(t.requiredTicket())) { // If player has the required ticket then the move to destination1 will be available
						Copyofplayerticket.replace(t.requiredTicket(),Copyofplayerticket.get(t.requiredTicket())-1);  //minus 1 ticket just used from the player's ticket board
					}  // We have our first move here
					else continue;

					for(int destination2 : setup.graph.adjacentNodes(destination1)){ //adjacent to destination1
						occupied = false;
						for(Player p : detectives){
							if(p.location() == destination2) occupied = true;
						}
						if(occupied) continue;
						for (ScotlandYard.Transport t2 : setup.graph.edgeValueOrDefault(destination1, destination2, ImmutableSet.of())){
							if(Copyofplayerticket.getOrDefault(t2.requiredTicket(),0) > 0) { //if the player still has enough ticket after the firstMove then the second move will be available
								doubleMoves.add(new Move.DoubleMove(mrX.piece(), source, t.requiredTicket(), destination1, t2.requiredTicket(), destination2));
								// make doublemove using information of the first move and second move
							}
						}
						if(Copyofplayerticket.get(ScotlandYard.Ticket.SECRET)>0) //if SECRET ticket can be used to make the secondMove
							doubleMoves.add(new Move.DoubleMove(mrX.piece(),source,t.requiredTicket(),destination1, ScotlandYard.Ticket.SECRET,destination2));

					}

				}
		     	if(mrX.has(ScotlandYard.Ticket.SECRET)) { //if SECRET ticket can be used for firstMove
		     		Copyofplayerticket.clear();
		     		Copyofplayerticket.putAll(mrX.tickets());// A muttable copy of player's ticket
					Copyofplayerticket.replace(ScotlandYard.Ticket.SECRET,Copyofplayerticket.get(ScotlandYard.Ticket.SECRET) - 1); //minus 1 SECRET ticket just used.
					// We now have information of the firstmove, which uses SECRET ticket
					for(int destination2 : setup.graph.adjacentNodes(destination1)){
						occupied = false;
						for(Player p : detectives){
							if(p.location() == destination2) occupied = true;
						}
						if(occupied) continue;
						for (ScotlandYard.Transport t2 : setup.graph.edgeValueOrDefault(destination1, destination2, ImmutableSet.of())){
							if(Copyofplayerticket.getOrDefault(t2.requiredTicket(),0) > 0) { //if the player still has enough ticket after the firstMove

								doubleMoves.add(new Move.DoubleMove(mrX.piece(), source, ScotlandYard.Ticket.SECRET, destination1, t2.requiredTicket(), destination2));
							}
						}
						if(Copyofplayerticket.get(ScotlandYard.Ticket.SECRET)>0) //if SECRET ticket can be used to make the secondMove
							doubleMoves.add(new Move.DoubleMove(mrX.piece(),source, ScotlandYard.Ticket.SECRET,destination1, ScotlandYard.Ticket.SECRET,destination2));

					}
				}


			 }
		     }
		     return ImmutableSet.copyOf(doubleMoves);
		 }

		// TODO
		 private final class MyGameState implements GameState {
		    private GameSetup setup;
		    private ImmutableSet<Piece> remaining;
		    private ImmutableList<LogEntry> log;
		    private Player mrX;
		    private List<Player> detectives;
		    private ImmutableList<Player> everyone;
		    private ImmutableSet<Move> moves;
		    private ImmutableSet<Piece> winner;
		    private int round;
			private MyGameState(final GameSetup setup, final ImmutableSet<Piece> remaining,final ImmutableList<LogEntry> log, final Player mrX, final List<Player> detectives, int round){
				 if(setup == null || remaining == null || log == null || mrX == null || detectives ==null) throw new NullPointerException();// check if the arguments passed in are Null.
				 CheckMrX(mrX);
				 CheckMoreThanOneMrX(detectives);
                 CheckDuplicateDetectives(detectives);
                 CheckLocationOverLapBetweenDetectives(detectives);
                 CheckTicketsDetectives(detectives);
                 CheckEmptyRoundsAndGraph(setup);
                 this.round = round;
			     this.setup = setup;
                 this.remaining = remaining;
                 this.log = log;
                 this.mrX = mrX;
			     this.detectives = detectives;
			     List<Player> copyofEveryone = new ArrayList<>();
			     copyofEveryone.add(mrX);
			     copyofEveryone.addAll(detectives);
			     everyone = ImmutableList.copyOf(copyofEveryone);
//                 Set<Move.SingleMove> AvailableSingleMoves = new HashSet<>();
//                 Set<Move.DoubleMove> AvailableDoubleMoves = new HashSet<>();
                 Set<Move> AvailableMoves = new HashSet<>();
				 if(remaining.contains(mrX.piece())){
					AvailableMoves.addAll(makeSingleMoves(setup, detectives, mrX, mrX.location()));
					AvailableMoves.addAll(makeDoubleMoves(setup, detectives, mrX, mrX.location()));
				 }
				 else{
				 	for (Piece detective : remaining){
				 		Player detectiveplayer = getPlayer(detective);
				 		AvailableMoves.addAll(makeSingleMoves(setup,detectives, detectiveplayer, detectiveplayer.location()));
					}
				 }

			     this.moves = ImmutableSet.copyOf(AvailableMoves);

			}
			public Player getPlayer(Piece piece){
				for (Player player : everyone){
					if (player.piece() == piece) return player;
				}
				return null;
			}

			@Override
			public GameSetup getSetup() {
				return setup;
			}
			@Override
			public ImmutableSet<Piece> getPlayers() {
				List<Piece> PieceList = new ArrayList<>();
				PieceList.add(mrX.piece());
				for (Player player : detectives){
					PieceList.add(player.piece());
				}
				return ImmutableSet.copyOf(PieceList);
			}
			@Override
			public Optional<Integer> getDetectiveLocation(Piece.Detective detective) {
                for (final var p : detectives){
                	if(p.piece() == detective) return Optional.of(p.location());
				}
				return Optional.empty();
			}
			@Override
			public Optional<Board.TicketBoard> getPlayerTickets(Piece piece){

				List<Player> Allplayer = new ArrayList<>();
				Allplayer.add(mrX);
				Allplayer.addAll(detectives);
				for (final Player p : Allplayer){
					if (p.piece() == piece) return Optional.of(ticket -> p.tickets().get(ticket));

				}
				return Optional.empty();
			}

			@Override
			public ImmutableList<LogEntry> getMrXTravelLog(){return log;}
			@Override
			public ImmutableSet<Piece> getWinner(){
				return ImmutableSet.of();
			}
			@Override
			public ImmutableSet<Move> getAvailableMoves(){
				return moves;
			}
			@Override
			public GameState advance(Move move) {
				if (!moves.contains(move))throw new IllegalArgumentException("Illegal move: " + move);
				Player NewPlayer = UpdatePlayer(move); //Update player that made the move
				Player newMrX = mrX;
				if(NewPlayer.isDetective()){
					newMrX = mrX.give(move.tickets());
				};
				//Make a new MrXTravelLog if mrX was the one made the move.
				ImmutableList<LogEntry> newMrXTravelLog = Updatelog(move);
				ImmutableSet<Piece> newRemaining = GetNewRemaining(move.commencedBy());
				if(newRemaining.isEmpty()) return new MyGameState(setup, ImmutableSet.of(mrX.piece()), newMrXTravelLog, newMrX, detectives, round +1);
				return new MyGameState(setup, newRemaining, newMrXTravelLog, newMrX, detectives, round);




			}
			public ImmutableList<LogEntry> Updatelog (Move move){
				if (move.commencedBy() == mrX.piece()) {
					return ImmutableList.copyOf(move.visit(new Move.Visitor<List<LogEntry>>() {
						List<LogEntry> MrXTravelLogToreturn = new ArrayList<>();

						@Override
						public List<LogEntry> visit(Move.SingleMove move) {
							MrXTravelLogToreturn.addAll(log);
							if (!revealRound(round)) {
								MrXTravelLogToreturn.add(LogEntry.hidden(move.ticket));
							} else MrXTravelLogToreturn.add(LogEntry.reveal(move.ticket, move.destination));
							return MrXTravelLogToreturn;
						}

						@Override
						public List<LogEntry> visit(Move.DoubleMove move) {
							MrXTravelLogToreturn.addAll(log);
							if (revealRound(round)) {
								MrXTravelLogToreturn.add(LogEntry.reveal(move.ticket1, move.destination1));
								MrXTravelLogToreturn.add(LogEntry.hidden(move.ticket2));
							}
							if (revealRound(round + 1)) {
								MrXTravelLogToreturn.add(LogEntry.hidden(move.ticket1));
								MrXTravelLogToreturn.add(LogEntry.reveal(move.ticket2, move.destination2));
							} else {
								MrXTravelLogToreturn.add(LogEntry.hidden(move.ticket1));
								MrXTravelLogToreturn.add(LogEntry.hidden(move.ticket2));
							}
							return MrXTravelLogToreturn;
						}
					}));
				}
				return log;
			}
			public Player UpdatePlayer(Move move){
				 return  move.visit(new Move.Visitor<Player>() {
					@Override
					public Player visit(Move.SingleMove move) {
						for (Player player : everyone) {
							if (player.piece() == move.commencedBy())
								return player.use(move.ticket).at(move.destination);
						}
						return null;
					}

					@Override
					public Player visit(Move.DoubleMove move) {
						for (Player player : everyone){
							if (player.piece() == move.commencedBy()){
								List<ScotlandYard.Ticket> ticketsUsed = new ArrayList<>();
								ticketsUsed.add(move.ticket1);
								ticketsUsed.add(move.ticket2);
								return player.use(ticketsUsed).at(move.destination2);
							}

						}
						return null;
					}

				});

			}
			public ImmutableSet<Piece> GetNewRemaining(Piece playerJustmove){
				List<Piece> NewRemainingToReturn = new ArrayList<>();
				if(playerJustmove == mrX.piece()) {
					for (Player detective : detectives) NewRemainingToReturn.add(detective.piece());
				}
				else{
					NewRemainingToReturn.addAll(remaining);
					NewRemainingToReturn.remove(playerJustmove);
				}
				return ImmutableSet.copyOf(NewRemainingToReturn);
			}
            public void CheckMrX(Player mrX){if(mrX.isDetective()) throw new IllegalArgumentException(); }
            public void CheckMoreThanOneMrX(final List<Player> detectives){
				for (Player player: detectives) {
					if(!player.isDetective()) throw new IllegalArgumentException();
				}
			}
			public void CheckDuplicateDetectives(final List<Player> detectives){
				List<Player> list = new ArrayList<>();
				for (Player player : detectives){
					if(list.contains(player)) throw new IllegalArgumentException();
					if(player != null) list.add(player);
				}

			}
			public void CheckLocationOverLapBetweenDetectives(final List<Player> detectives){
				List<Integer> list = new ArrayList<>();
				for (Player player : detectives){
					if(list.contains(player.location())) throw new IllegalArgumentException();
					if(player != null) list.add(player.location());
				}
			}
			public void CheckTicketsDetectives(final List<Player> detectives){
				for (Player player : detectives){
					if(player.has(ScotlandYard.Ticket.SECRET)) throw new IllegalArgumentException();
					if(player.has(ScotlandYard.Ticket.DOUBLE)) throw new IllegalArgumentException();
				}
			}
			public void CheckEmptyRoundsAndGraph(final GameSetup setup){
                 if (setup.rounds.size() <= 0) throw new IllegalArgumentException();
                 if (setup.graph.nodes().size() <= 0) throw new IllegalArgumentException();
			}
			public boolean revealRound(int round){
				return setup.rounds.get(round-1);
			}


		}




	}


