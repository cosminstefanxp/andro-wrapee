Andro-Wrapee
============
#Description
The library users can perform the CRUD (create, read, update, delete) operations on a database that stores Java objects. Practically, it's a DAO (Database Access Object) that can help users manage SQLite tables which contain Java objects.

#Documentation
The Javadoc can be found [here](http://andro-wrapee.googlecode.com/git/doc/index.html).

##Use scenario
Here are the simple steps necessary for using the library.

#### Defining the classes that should be stored in the database
    @DatabaseClass     //required annotation for classed stored in database
    public class Car {
    
    @IdField
    public long id; //will be stored in the database as the primary key of the table
    
    @DatabaseField
    private String name; //will be stored in the database
    
    @DatabaseField
    protected Date producedOn; //will be stored in the database
    
    public ArrayList<Color> availableColors; //will not be stored in the database, as it doesn't have the required annotation
    ...
}

#### Get a reference to the database helper
    SQLiteOpenHelper dbHelper = new DefaultDatabaseHelper(context, DATABASE_NAME, DATABASE_VERSION, 
        new Class[] { Car.class, ... }, //The classes to store in the database
        new String[] { "cars", ... }); // The table names for the classes in the database

#### Get a reference to the ReflectionManager
    ReflectionManager rm = new ReflectionManager(Car.class);

#### Get the reference to the DAO
    DefaultDao<Car> dao = new DefaultDAO<Car>(Car.class, dbHelper, rm, "cars");

#### Use the DAO
    Car newCar=new Car();
    ...
    dao.open();
    long id=dao.insert(newCar);
    newCar.id=id;
    dao.close();
    ...
    dao.open();
    dao.update(newCar, newCar.id);
    
    long carID=...
    Car otherCar=dao.fetch(carID);
    dao.close();
    ...

For more details, consult the Javadoc of the [DefaultDao](http://andro-wrapee.googlecode.com/git/doc/org/androwrapee/db/DefaultDAO.html) object.
