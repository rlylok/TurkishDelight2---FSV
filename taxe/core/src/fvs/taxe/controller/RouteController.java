package fvs.taxe.controller;

import fvs.taxe.StationClickListener;
import fvs.taxe.TaxeGame;
import gameLogic.GameState;
import gameLogic.map.CollisionStation;
import gameLogic.map.Connection;
import gameLogic.map.IPositionable;
import gameLogic.map.Station;
import gameLogic.resource.Train;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

public class RouteController {
    private Context context;
    private Group routingButtons = new Group();
    private List<IPositionable> positions;
    private List<Connection> connections;
    private boolean isRouting = false;
    private Train train;
    private boolean canEndRouting = true;

    public RouteController(Context context) {
        this.context = context;

        StationController.subscribeStationClick(new StationClickListener() {
            @Override
            public void clicked(Station station) {
                if (isRouting) {
                    addStationToRoute(station);
                }
            }
        });
    }

    public void begin(Train train) {
        this.train = train;
        isRouting = true;
        positions = new ArrayList<IPositionable>();
        connections = new ArrayList<Connection>();
        positions.add(train.getPosition());
        context.getGameLogic().setState(GameState.ROUTING);
        addRoutingButtons();

        TrainController trainController = new TrainController(context);
        trainController.setTrainsVisible(train, false);
        train.getActor().setVisible(true);
    }

    private void addStationToRoute(Station station) {
        // the latest position chosen in the positions so far
        IPositionable lastPosition =  positions.get(positions.size() - 1);
        Station lastStation = context.getGameLogic().getMap().getStationFromPosition(lastPosition);

        boolean hasConnection = context.getGameLogic().getMap().doesConnectionExist(station.getName(), lastStation.getName());
        if(!hasConnection) {
            context.getTopBarController().displayFlashMessage("This connection doesn't exist", Color.RED);
        } else {
            positions.add(station.getLocation());
            connections.add(context.getGameLogic().getMap().getConnection(station.getName(), lastStation.getName()));
            canEndRouting = !(station instanceof CollisionStation);
        }
    }

    private void addRoutingButtons() {
        TextButton doneRouting = new TextButton("Route Complete", context.getSkin());
        TextButton cancel = new TextButton("Cancel", context.getSkin());

        doneRouting.setPosition(TaxeGame.WIDTH - 250, TaxeGame.HEIGHT - 33);
        cancel.setPosition(TaxeGame.WIDTH - 100, TaxeGame.HEIGHT - 33);

        cancel.addListener(new ClickListener() {
            @Override
            public void clicked (InputEvent event, float x, float y) {
                endRouting();
            }
        });

        doneRouting.addListener(new ClickListener() {
            @Override
            public void clicked (InputEvent event, float x, float y) {
                if(!canEndRouting) {
                    context.getTopBarController().displayFlashMessage("Your route must end at a station", Color.RED);
                    return;
                }

                confirmed();
                endRouting();
            }
        });

        routingButtons.addActor(doneRouting);
        routingButtons.addActor(cancel);

        context.getStage().addActor(routingButtons);
    }

    private void confirmed() {
        train.setRoute(context.getGameLogic().getMap().createRoute(positions));

        TrainMoveController move = new TrainMoveController(context, train);
    }

    private void endRouting() {
        context.getGameLogic().setState(GameState.NORMAL);
        routingButtons.remove();
        isRouting = false;

        TrainController trainController = new TrainController(context);
        trainController.setTrainsVisible(train, true);
        train.getActor().setVisible(false);
        
        drawRoute(Color.GRAY);
    }

    public void drawRoute(Color color) {
        for (Connection connection : connections) {
        	connection.getActor().setConnectionColor(color);
        }
    }
}
