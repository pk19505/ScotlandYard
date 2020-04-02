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
            return new MyGameState(setup, ImmutableSet.of(Piece.MrX.MRX), ImmutableList.of(), mrX, detectives);

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
		     if (mrX.has(ScotlandYard.Ticket.DOUBLE)){

		     for (int destination1 : setup.graph.adjacentNodes(source)){ //All the adjacentNodes to source
		     	var occupied = false;
		     	for (Player p : detectives){                                //Check if the destination is occupied by a detective.
					if(p.location() == destination1) occupied = true;       // If it is then the move should not be available and
				}	                                                        //should move onto checking for the next potential destination
		     	if(occupied) continue;

		     	for (ScotlandYard.Transport t : setup.graph.edgeValueOrDefault(source, destination1, ImmutableSet.of())) { //All the transports from source to destination1
					Copyofplayerticket.clear();
					Copyofplayerticket.putAll(mrX.tickets());    // Make a muttable copy of player ticket
					if(mrX.has(t.requiredTicket())) { // If player has the required ticket then the move to destination1 will be available
						Move.SingleMove firstMove = new Move.SingleMove(mrX.piece(), source, t.requiredTicket(), destination1);
						Copyofplayerticket.replace(t.requiredTicket(),Copyofplayerticket.get(t.requiredTicket())-1);  //minus 1 ticket just used
					}
					for(int destination2 : setup.graph.adjacentNodes(destination1)){
						occupied = false;
						for(Player p : detectives){
							if(p.location() == destination2) occupied = true;
						}
						if(occupied) continue;
						for (ScotlandYard.Transport t2 : setup.graph.edgeValueOrDefault(destination1, destination2, ImmutableSet.of())){
							if(Copyofplayerticket.getOrDefault(t2.requiredTicket(),0) > 0) { //if the player still has enough ticket after the firstMove
								Move.SingleMove SecondMove = new Move.SingleMove(mrX.piece(), destination1, t2.requiredTicket(), destination2); //make a seondmove using t2 from destination1 to destination2
								doubleMoves.add(new Move.DoubleMove(mrX.piece(), source, t.requiredTicket(), destination1, t2.requiredTicket(), destination2));
							}
						}
						if(Copyofplayerticket.get(ScotlandYard.Ticket.SECRET)>0) //if SECRET ticket can be used to make the secondMove
							doubleMoves.add(new Move.DoubleMove(mrX.piece(),source,t.requiredTicket(),destination1, ScotlandYard.Ticket.SECRET,destination2));

					}

				}

		     	if(mrX.has(ScotlandYard.Ticket.SECRET)) { //if SECRET ticket can be used for firstMove
		     		Copyofplayerticket.clear();
		     		Copyofplayerticket.putAll(mrX.tickets());// A muttable copy of player's ticket
					Move.SingleMove FirstMove = new Move.SingleMove(mrX.piece(), source, ScotlandYard.Ticket.SECRET, destination1);//make a firstMove using SECRET TICKET from source to destination1
					Copyofplayerticket.replace(ScotlandYard.Ticket.SECRET,Copyofplayerticket.get(ScotlandYard.Ticket.SECRET) - 1);
					for(int destination2 : setup.graph.adjacentNodes(destination1)){
						occupied = false;
						for(Player p : detectives){
							if(p.location() == destination2) occupied = true;
						}
						if(occupied) continue;
						for (ScotlandYard.Transport t2 : setup.graph.edgeValueOrDefault(destination1, destination2, ImmutableSet.of())){
							if(Copyofplayerticket.getOrDefault(t2.requiredTicket(),0) > 0) { //if the player still has enough ticket after the firstMove
								Move.SingleMove SecondMove = new Move.SingleMove(mrX.piece(), destination1, t2.requiredTicket(), destination2);
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
			private MyGameState(final GameSetup setup, final ImmutableSet<Piece> remaining,final ImmutableList<LogEntry> log, final Player mrX, final List<Player> detectives){
				 if(setup == null || remaining == null || log == null || mrX == null || detectives ==null) throw new NullPointerException();// check if the arguments passed in are Null.
				 CheckMrX(mrX);
				 CheckMoreThanOneMrX(detectives);
                 CheckDuplicateDetectives(detectives);
                 CheckLocationOverLapBetweenDetectives(detectives);
                 CheckTicketsDetectives(detectives);
                 CheckEmptyRoundsAndGraph(setup);
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
//			     for (Player player : everyone){
			     	AvailableMoves.addAll(makeSingleMoves(setup, detectives, mrX, mrX.location()));
			     	AvailableMoves.addAll(makeDoubleMoves(setup, detectives, mrX, mrX.location()));
//				 }
			     this.moves = ImmutableSet.copyOf(AvailableMoves);

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
			public ImmutableList<LogEntry> getMrXTravelLog(){return null;}
			@Override
			public ImmutableSet<Piece> getWinner(){
				return ImmutableSet.of();
			}
			@Override
			public ImmutableSet<Move> getAvailableMoves(){
				return moves;
			}
			@Override
			public GameState advance(Move move) { return null; }
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


		}




	}


