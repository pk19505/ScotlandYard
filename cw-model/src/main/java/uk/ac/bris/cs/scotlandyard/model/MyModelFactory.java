package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableSet;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Factory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * cw-model
 * Stage 2: Complete this class
 */
public final class MyModelFactory implements Factory<Model> {

	@Nonnull @Override public Model build(GameSetup setup,
	                                      Player mrX,
	                                      ImmutableList<Player> detectives) {
		// TODO
		return new Model() {
			List<Observer> observerList = new ArrayList<>();
			MyGameStateFactory f = new MyGameStateFactory();
			Board.GameState State = f.build(setup,mrX,detectives);
			@Nonnull
			@Override
			public Board getCurrentBoard() {
				return State;
			}

			@Override
			public void registerObserver(@Nonnull Observer observer) {
				if(observer == null) throw new NullPointerException();
				if(observerList.contains(observer)) throw new IllegalArgumentException();
				observerList.add(observer);
			}

			@Override
			public void unregisterObserver(@Nonnull Observer observer) {
				if(observer == null) throw  new NullPointerException();
				if(!observerList.contains(observer)) throw new IllegalArgumentException();
				observerList.remove(observer);
			}

			@Nonnull
			@Override
			public ImmutableSet<Observer> getObservers() {
				return ImmutableSet.copyOf(observerList);
			}

			@Override
			public void chooseMove(@Nonnull Move move) {
				State = State.advance(move);
				Observer.Event event;
				if(State.getWinner().isEmpty()) event = Observer.Event.MOVE_MADE;
				else event = Observer.Event.GAME_OVER;
				for (Observer o : observerList) o.onModelChanged(State, event);
			}
		};

	}

}
